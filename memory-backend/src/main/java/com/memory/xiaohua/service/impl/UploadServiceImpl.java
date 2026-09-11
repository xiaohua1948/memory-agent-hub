package com.memory.xiaohua.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.config.UploadConfig;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.FaceMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.ImageMetaDataMapper;
import com.memory.xiaohua.mapper.UserFileMapper;
import com.memory.xiaohua.mapper.VideoMetaDataMapper;
import com.memory.xiaohua.model.dto.upload.UploadChunkVO;
import com.memory.xiaohua.model.dto.upload.UploadCompleteRequest;
import com.memory.xiaohua.model.dto.upload.UploadCompleteVO;
import com.memory.xiaohua.model.dto.upload.UploadPreCheckRequest;
import com.memory.xiaohua.model.dto.upload.UploadPreCheckVO;
import com.memory.xiaohua.model.entity.Face;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.ImageMetaData;
import com.memory.xiaohua.model.entity.UploadTask;
import com.memory.xiaohua.model.entity.UserFileEntity;
import com.memory.xiaohua.model.entity.VideoMetaData;
import com.memory.xiaohua.mq.FileAnalysisProducer;
import com.memory.xiaohua.mq.message.FileAnalysisMessage;
import com.memory.xiaohua.service.AlbumService;
import com.memory.xiaohua.service.RecordService;
import com.memory.xiaohua.service.UploadService;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.file.FileUtil;
import com.memory.xiaohua.utils.file.MinioMultipartUtil;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import com.memory.xiaohua.utils.metaData.ImageMetadataParserUtil;
import com.memory.xiaohua.utils.metaData.VideoMetadataParserUtil;
import io.minio.messages.Part;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分片上传服务实现
 *
 * <p>设计要点：<br>
 * 1. <b>秒传</b>：以整个文件 MD5 为准，命中即直接建立用户-文件关联；<br>
 * 2. <b>断点续传</b>：已上传分片以 MinIO {@code listMultipart} 为权威数据源，Redis 挂掉也不丢进度；<br>
 * 3. <b>同文件共享任务</b>：相同 MD5 复用同一个 multipart 任务（内容必然相同），天然规避并发重复上传；<br>
 * 4. <b>失败补偿</b>：合并后任何一步失败都删除已合并对象，避免留下无主文件。</p>
 */
@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    /** S3 multipart 分片数上限 */
    private static final int MAX_PARTS = 10000;

    private static final String TASK_KEY_PREFIX = "upload:task:";
    private static final String MD5_KEY_PREFIX = "upload:md5:";
    private static final String LOCK_KEY_PREFIX = "upload:lock:";
    private static final String USER_TASK_KEY_PREFIX = "upload:user:";

    /**
     * 存活上传任务表：uploadId -> "objectName|minioUploadId"。
     * 不带 TTL，用于在任务元数据过期后仍能定位到 MinIO 上的残留分片并中止它。
     */
    private static final String ALIVE_TASK_KEY = "upload:alive";

    private static final String ALIVE_VALUE_SEPARATOR = "|";

    /** 每任务已上传分片序号集合（仅进度展示，非权威数据源，权威以 MinIO listParts 为准） */
    private static final String CHUNK_SET_KEY_PREFIX = "upload:chunks:";

    /** 秒传并发锁持有时间 */
    private static final long LOCK_SECONDS = 30L;

    @Resource
    private UploadConfig uploadConfig;
    @Resource
    private MinioMultipartUtil minioMultipartUtil;
    @Resource
    private MinioOSSUtil minioOSSUtil;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private ImageMetadataParserUtil imageMetadataParserUtil;
    @Resource
    private VideoMetadataParserUtil videoMetadataParserUtil;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private UserFileMapper userFileMapper;
    @Resource
    private FaceMapper faceMapper;
    @Resource
    private ImageMetaDataMapper imageMetadataMapper;
    @Resource
    private VideoMetaDataMapper videoMetaDataMapper;
    @Resource
    private AlbumService albumService;
    @Resource
    private RecordService recordService;
    @Resource
    private RedisUtil redisUtil;

    @Resource
    private FileAnalysisProducer fileAnalysisProducer;

    @Resource
    private ObjectMapper objectMapper;

    // ==================== 阶段一：预检 ====================

    @Override
    public UploadPreCheckVO preCheck(Long userId, UploadPreCheckRequest request) {
        String filename = request.getFilename();
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "文件名不能为空");
        }
        long size = request.getSize() == null ? 0L : request.getSize();
        if (size <= 0) {
            throw new BusinessException(StatusCode.FILE_EMPTY, "文件内容为空");
        }
        if (size > uploadConfig.getMaxFileSize()) {
            throw new BusinessException(StatusCode.FILE_TOO_LARGE,
                    "文件超过大小限制（最大 " + formatSize(uploadConfig.getMaxFileSize()) + "）");
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new BusinessException(StatusCode.FILE_TYPE_NOT_SUPPORT, "文件格式错误，缺少扩展名");
        }
        String suffix = filename.substring(dotIndex + 1).toLowerCase();
        String category = fileUtil.getFileType(suffix);
        if (category.isEmpty()) {
            throw new BusinessException(StatusCode.FILE_TYPE_NOT_SUPPORT, "不支持的文件格式：." + suffix);
        }

        String md5 = request.getMd5();
        if (!StringUtils.hasText(md5)) {
            throw new BusinessException(StatusCode.UPLOAD_PARAM_MISSING, "缺少文件 MD5");
        }
        md5 = md5.toLowerCase();

        Long albumId = request.getAlbumId() == null ? -1L : request.getAlbumId();
        int totalChunks = uploadConfig.calcTotalChunks(size);
        if (totalChunks > MAX_PARTS) {
            throw new BusinessException(StatusCode.FILE_TOO_LARGE, "文件过大，分片数量超出限制");
        }

        // 秒传命中则直接结束
        String instantFileId = instantUpload(userId, md5, albumId);
        UploadPreCheckVO vo = new UploadPreCheckVO();
        if (instantFileId != null) {
            vo.setUploaded(true);
            vo.setFileId(instantFileId);
            return vo;
        }

        UploadTask task = resolveTask(userId, request, suffix, category, md5, size, totalChunks, albumId);

        vo.setUploaded(false);
        vo.setUploadId(task.getUploadId());
        vo.setChunkSize(task.getChunkSize());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setUploadedChunks(listUploadedChunkIndexes(task));
        vo.setExpiresAt(task.getCreatedAt() + TimeUnit.HOURS.toMillis(uploadConfig.getTaskTtlHours()));
        return vo;
    }

    /**
     * 复用已有任务或创建新任务。
     * 相同 MD5 的任务可被不同用户共享——内容相同则分片必然相同，共享是安全的，
     * 且能避免并发上传同一文件时产生两个 multipart 任务互相覆盖。
     */
    private UploadTask resolveTask(Long userId, UploadPreCheckRequest request, String suffix,
                                   String category, String md5, long size, int totalChunks, Long albumId) {
        // 1. 前端携带了 uploadId 且任务仍有效 → 续传
        if (StringUtils.hasText(request.getUploadId())) {
            UploadTask existing = loadTask(request.getUploadId());
            if (existing != null && md5.equals(existing.getFileMd5())
                    && existing.getUserId().equals(userId)) {
                existing.setAlbumId(albumId);
                saveTask(existing);
                return existing;
            }
        }

        // 2. 同一用户同一 MD5 已有进行中的任务 → 复用（跨用户不共享，避免越权复用他人任务）
        Object cachedUploadId = redisUtil.get(MD5_KEY_PREFIX + md5);
        if (cachedUploadId != null) {
            UploadTask shared = loadTask(String.valueOf(cachedUploadId));
            if (shared != null && shared.getUserId().equals(userId)
                    && md5.equals(shared.getFileMd5()) && shared.getFileSize() == size) {
                shared.setAlbumId(albumId);
                saveTask(shared);
                registerUserTask(userId, shared.getUploadId());
                return shared;
            }
        }

        // 3. 新建任务
        checkConcurrentLimit(userId);
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String date = LocalDate.now().toString();
        String fileObjectName = "file/" + date + "/" + md5 + "." + suffix;
        String thumbnailObjectName = "thumbnail/" + date + "/" + md5 + ".jpg";
        String minioUploadId = minioMultipartUtil.createMultipartUpload(
                fileObjectName, resolveContentType(suffix, category));

        UploadTask task = new UploadTask();
        task.setUploadId(uploadId);
        task.setUserId(userId);
        task.setFileMd5(md5);
        task.setOriginFileName(request.getFilename());
        task.setSuffix(suffix);
        task.setCategory(category);
        task.setContentType(resolveContentType(suffix, category));
        task.setFileSize(size);
        task.setChunkSize(uploadConfig.getChunkSize());
        task.setTotalChunks(totalChunks);
        task.setAlbumId(albumId);
        task.setLastModified(request.getLastModified());
        task.setFileObjectName(fileObjectName);
        task.setThumbnailObjectName(thumbnailObjectName);
        task.setMinioUploadId(minioUploadId);
        task.setCreatedAt(System.currentTimeMillis());

        saveTask(task);
        registerUserTask(userId, uploadId);
        registerAliveTask(task);
        redisUtil.set(MD5_KEY_PREFIX + md5, uploadId, uploadConfig.getTaskTtlHours(), TimeUnit.HOURS);
        return task;
    }

    /**
     * 秒传：文件已存在则建立用户关联，返回 fileId；不存在返回 null。
     * 用 Redis 锁保证"查询 + 建关联"的原子性，避免并发下重复插入。
     */
    private String instantUpload(Long userId, String md5, Long albumId) {
        String lockKey = LOCK_KEY_PREFIX + md5;
        boolean locked = redisUtil.setIfAbsent(lockKey, "1", LOCK_SECONDS, TimeUnit.SECONDS);
        if (!locked) {
            // 已有线程在处理同一文件，稍等后读取其结果
            try {
                Thread.sleep(150L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            String fileId = fileMapper.isFileExist(md5);
            if (fileId == null) {
                return null;
            }
            // 兜底校验：file 主表可能残留但 OSS 对象已被彻底删除（异步删除时序差），
            // 此时不应秒传，否则返回一个指向已删除 OSS 对象的 fileId。
            String objectName = fileMapper.getObjectNameByMd5(md5);
            if (objectName != null && !minioOSSUtil.exists(objectName)) {
                // 对象已删：清理可能残留的秒传缓存，走正常上传流程
                redisUtil.delete(MD5_KEY_PREFIX + md5);
                return null;
            }
            ensureUserFileAssociation(userId, fileId, albumId);
            return fileId;
        } finally {
            if (locked) {
                redisUtil.delete(lockKey);
            }
        }
    }

    // ==================== 阶段二：上传分片 ====================

    @Override
    public UploadChunkVO uploadChunk(Long userId, String uploadId, int chunkIndex, MultipartFile chunk) {
        UploadTask task = requireTask(userId, uploadId);

        if (chunk == null || chunk.isEmpty()) {
            throw new BusinessException(StatusCode.FILE_EMPTY, "分片内容为空");
        }
        if (chunkIndex < 0 || chunkIndex >= task.getTotalChunks()) {
            throw new BusinessException(StatusCode.CHUNK_INDEX_INVALID, "分片序号非法：" + chunkIndex);
        }
        if (!uploadConfig.isChunkSizeValid(chunkIndex, task.getTotalChunks(), chunk.getSize())) {
            throw new BusinessException(StatusCode.CHUNK_SIZE_INVALID,
                    "分片大小与约定不符，期望 " + task.getChunkSize() + " 字节，实际 " + chunk.getSize());
        }

        try {
            minioMultipartUtil.uploadPart(task.getFileObjectName(), task.getMinioUploadId(),
                    chunkIndex + 1, chunk.getInputStream(), chunk.getSize());
        } catch (IOException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "读取分片数据失败: " + e.getMessage());
        }

        // 续期：只要还在传分片，任务就不该过期
        touchTask(task);

        // 进度计数走 Redis 集合，避免每次分片都全量 listParts（O(n²) 请求）；权威数据源仍是 MinIO
        String chunkSetKey = CHUNK_SET_KEY_PREFIX + uploadId;
        redisUtil.sAdd(chunkSetKey, chunkIndex);
        redisUtil.expire(chunkSetKey, uploadConfig.getTaskTtlHours(), TimeUnit.HOURS);
        long uploadedCount = redisUtil.sSize(chunkSetKey);

        UploadChunkVO vo = new UploadChunkVO();
        vo.setChunkIndex(chunkIndex);
        vo.setUploadedCount((int) uploadedCount);
        vo.setTotalChunks(task.getTotalChunks());
        vo.setAllChunksDone(uploadedCount == task.getTotalChunks());
        return vo;
    }

    // ==================== 阶段三：合并入库 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadCompleteVO complete(Long userId, UploadCompleteRequest request, HttpServletRequest httpRequest) {
        // 秒传兜底前置：并发上传同一文件时，另一请求可能已合并入库并清理了任务，
        // 此时直接返回已入库文件，而不是抛"任务不存在"。
        String instantFileId = instantUpload(userId, request.getMd5(), null);
        if (instantFileId != null) {
            UploadCompleteVO instantVo = new UploadCompleteVO();
            instantVo.setFileId(instantFileId);
            instantVo.setInstant(true);
            return instantVo;
        }

        UploadTask task = requireTask(userId, request.getUploadId());
        if (!task.getFileMd5().equalsIgnoreCase(request.getMd5())) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "文件 MD5 与上传任务不匹配");
        }

        List<Part> parts = minioMultipartUtil.listParts(task.getFileObjectName(), task.getMinioUploadId(), task.getTotalChunks());
        if (parts.size() != task.getTotalChunks()) {
            throw new BusinessException(StatusCode.CHUNK_MISSING,
                    "分片未上传完整：" + parts.size() + "/" + task.getTotalChunks());
        }
        Part[] sortedParts = parts.stream()
                .sorted(Comparator.comparingInt(Part::partNumber))
                .toArray(Part[]::new);

        minioMultipartUtil.completeMultipart(task.getFileObjectName(), task.getMinioUploadId(), sortedParts);

        // 已进入"对象已生成"状态，后续失败必须清理，否则会留下无数据库的孤儿文件
        try {
            clearTask(task);
            verifyMd5(task);

            LocalDateTime lastModifiedTime = parseLastModified(task.getLastModified());
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileId(UUID.randomUUID().toString().replace("-", ""));
            fileEntity.setOriginFileName(task.getOriginFileName());
            fileEntity.setSize(task.getFileSize());
            fileEntity.setLastModifiedTime(lastModifiedTime);
            fileEntity.setContentType(task.getContentType());
            fileEntity.setCategory(task.getCategory());
            fileEntity.setMd5(task.getFileMd5());
            fileEntity.setFileObjectName(task.getFileObjectName());
            fileEntity.setThumbnailObjectName(task.getThumbnailObjectName());

            ImageMetaData imageMeta = null;
            VideoMetaData videoMeta = null;
            if ("image".equals(task.getCategory())) {
                imageMeta = fillImageMeta(task, fileEntity, lastModifiedTime);
            } else {
                videoMeta = fillVideoMeta(task, fileEntity, lastModifiedTime);
            }

            fileEntity.setFileUrl(minioOSSUtil.getFileUrl(task.getFileObjectName()));
            fileEntity.setThumbnailUrl(minioOSSUtil.getFileUrl(task.getThumbnailObjectName()));

            // 落库去重：并发 complete 对同一 MD5 文件会各自走到这里，用 MD5 锁串行化
            // "检查是否已入库 + 落库"，命中的一方降级为秒传，避免 file 表出现同 MD5 重复行。
            String existingFileId = persistWithDedup(userId, task, fileEntity, imageMeta, videoMeta);
            if (existingFileId != null) {
                UploadCompleteVO instantVo = new UploadCompleteVO();
                instantVo.setFileId(existingFileId);
                instantVo.setInstant(true);
                return instantVo;
            }

            recordService.createRecordLog("上传照片:" + task.getOriginFileName(), 1, userId, httpRequest);

            // 上传主流程到此结束（存对象 + 落库），耗时分析改为异步事件
            registerAnalysisTasks(fileEntity, userId);

            UploadCompleteVO vo = new UploadCompleteVO();
            vo.setFileId(fileEntity.getFileId());
            vo.setOriginFileName(fileEntity.getOriginFileName());
            vo.setCategory(fileEntity.getCategory());
            vo.setFileUrl(fileEntity.getFileUrl());
            vo.setThumbnailUrl(fileEntity.getThumbnailUrl());
            vo.setWidth(fileEntity.getWidth());
            vo.setHeight(fileEntity.getHeight());
            vo.setSize(fileEntity.getSize());
            vo.setInstant(false);
            return vo;
        } catch (DuplicateKeyException e) {
            // 并发下另一请求已把同一 MD5 文件落库（同 MD5 对象名相同、对象共享），
            // 此处绝不能清理对象，否则会破坏对方已入库文件；提示重试，重试时秒传命中。
            log.warn("并发落库冲突，文件已由其他请求入库: md5={}", task.getFileMd5());
            throw new BusinessException(StatusCode.UPLOAD_TASK_NOT_FOUND, "文件已存在，请重新上传");
        } catch (Exception e) {
            log.error("上传合并后处理失败，清理已生成对象: {}", task.getFileObjectName(), e);
            safeDelete(task.getFileObjectName());
            safeDelete(task.getThumbnailObjectName());
            throw e;
        }
    }

    /**
     * 上传落库成功后，按需投递异步分析事件（逆地理编码 / 图像分类识别 / 人脸检测）。
     *
     * <p>这些分析耗时且结果不阻塞用户查看，交由来独立消费者组处理，上传接口只保留
     * "存对象 + 落库"两件必须同步完成的事。消息在事务提交后发送，避免事务回滚后
     * 消费者读到尚不存在的文件。</p>
     */
    private void registerAnalysisTasks(FileEntity fileEntity, Long userId) {
        String fileId = fileEntity.getFileId();
        String category = fileEntity.getCategory();
        boolean isImage = "image".equals(category);
        boolean hasThumbnail = fileEntity.getThumbnailObjectName() != null;
        boolean hasGeo = fileEntity.getLatitude() != null && fileEntity.getLongitude() != null;

        List<Runnable> tasks = new ArrayList<>();
        if (hasGeo) {
            FileAnalysisMessage message = FileAnalysisMessage.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .category(category)
                    .longitude(fileEntity.getLongitude())
                    .latitude(fileEntity.getLatitude())
                    .build();
            tasks.add(() -> fileAnalysisProducer.sendGeocode(message));
        }
        if (isImage && hasThumbnail) {
            FileAnalysisMessage message = FileAnalysisMessage.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .category(category)
                    .thumbnailObjectName(fileEntity.getThumbnailObjectName())
                    .build();
            tasks.add(() -> fileAnalysisProducer.sendImageTag(message));
        }
        if (hasThumbnail) {
            FileAnalysisMessage message = FileAnalysisMessage.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .category(category)
                    .thumbnailObjectName(fileEntity.getThumbnailObjectName())
                    .build();
            tasks.add(() -> fileAnalysisProducer.sendFace(message));
        }
        if (tasks.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tasks.forEach(Runnable::run);
                }
            });
        } else {
            tasks.forEach(Runnable::run);
        }
    }

    // ==================== 取消与清理 ====================

    @Override
    public void abort(Long userId, String uploadId) {
        UploadTask task = loadTask(uploadId);
        if (task == null) {
            return;
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(StatusCode.NO_AUTH_ERROR, "无权操作该上传任务");
        }
        minioMultipartUtil.abortMultipart(task.getFileObjectName(), task.getMinioUploadId());
        redisUtil.delete(TASK_KEY_PREFIX + uploadId);
        redisUtil.sRemove(USER_TASK_KEY_PREFIX + task.getUserId(), uploadId);
        redisUtil.hdel(ALIVE_TASK_KEY, uploadId);
        redisUtil.delete(CHUNK_SET_KEY_PREFIX + uploadId);
        Object cachedUploadId = redisUtil.get(MD5_KEY_PREFIX + task.getFileMd5());
        if (cachedUploadId != null && uploadId.equals(String.valueOf(cachedUploadId))) {
            redisUtil.delete(MD5_KEY_PREFIX + task.getFileMd5());
        }
    }

    /**
     * 清理已放弃的上传任务。
     *
     * <p>任务元数据（Redis，带 TTL）过期后即判定为"用户已放弃"，
     * 此时从存活表中取出对象名与 MinIO uploadId，中止其 multipart 上传，
     * 回收已上传的分片。</p>
     */
    @Override
    public int cleanExpiredTasks() {
        int count = 0;
        Map<String, Object> aliveTasks = redisUtil.hgetAll(ALIVE_TASK_KEY);
        for (Map.Entry<String, Object> entry : aliveTasks.entrySet()) {
            String uploadId = String.valueOf(entry.getKey());
            // 任务元数据仍在有效期内，说明用户可能还在传，跳过
            if (redisUtil.exists(TASK_KEY_PREFIX + uploadId)) {
                continue;
            }
            String[] segments = String.valueOf(entry.getValue()).split("\\" + ALIVE_VALUE_SEPARATOR, 2);
            if (segments.length == 2) {
                minioMultipartUtil.abortMultipart(segments[0], segments[1]);
                count++;
            }
            redisUtil.hdel(ALIVE_TASK_KEY, uploadId);
        }
        return count;
    }

    // ==================== 内部方法 ====================

    /**
     * 图片分支：一次读出字节，复用给 EXIF 解析与缩略图压缩，避免重复网络读取。
     */
    private ImageMetaData fillImageMeta(UploadTask task, FileEntity fileEntity, LocalDateTime lastModifiedTime) {
        byte[] bytes = minioOSSUtil.getFileBytes(task.getFileObjectName());

        ImageMetaData imageMetadata = new ImageMetaData();
        try {
            imageMetadata = imageMetadataParserUtil.getImageMetadata(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            log.warn("图片元数据解析失败: {}", task.getOriginFileName(), e);
        }
        if (imageMetadata.getDateTimeOriginal() == null) {
            imageMetadata.setDateTimeOriginal(lastModifiedTime);
        }

        byte[] thumbnail = task.getFileSize() > uploadConfig.getThumbnailThreshold()
                ? compressImage(bytes)
                : bytes;
        if (thumbnail == null || thumbnail.length == 0) {
            thumbnail = bytes;
        }
        minioOSSUtil.uploadToOSS(task.getThumbnailObjectName(),
                new ByteArrayInputStream(thumbnail), thumbnail.length, "image/jpeg");

        fileEntity.setDateTimeOriginal(imageMetadata.getDateTimeOriginal());
        fileEntity.setWidth(imageMetadata.getWidth());
        fileEntity.setHeight(imageMetadata.getHeight());
        fileEntity.setMake(imageMetadata.getMake());
        fileEntity.setModel(imageMetadata.getModel());
        fileEntity.setLatitude(imageMetadata.getLatitude());
        fileEntity.setLatitudeRef(imageMetadata.getLatitudeRef());
        fileEntity.setLongitude(imageMetadata.getLongitude());
        fileEntity.setLongitudeRef(imageMetadata.getLongitudeRef());

        // 外键依赖 file 主表，实际插入延迟到 persistFile 之后
        imageMetadata.setFileId(fileEntity.getFileId());
        return imageMetadata;
    }

    /**
     * 视频分支：全程流式处理，不把整个视频读进内存。
     */
    private VideoMetaData fillVideoMeta(UploadTask task, FileEntity fileEntity, LocalDateTime lastModifiedTime) {
        VideoMetaData videoMetadata = new VideoMetaData();
        try (InputStream in = minioOSSUtil.getObjectStream(task.getFileObjectName())) {
            videoMetadata = videoMetadataParserUtil.GetVideoMetadata(in);
        } catch (Exception e) {
            log.warn("视频元数据解析失败: {}", task.getOriginFileName(), e);
        }
        if (videoMetadata.getDateTimeOriginal() == null) {
            videoMetadata.setDateTimeOriginal(lastModifiedTime);
        }

        byte[] cover = null;
        try (InputStream in = minioOSSUtil.getObjectStream(task.getFileObjectName())) {
            cover = videoMetadataParserUtil.extractCover(in);
        } catch (Exception e) {
            log.warn("视频封面提取失败: {}", task.getOriginFileName(), e);
        }
        if (cover == null || cover.length == 0) {
            // MinIO 不接受 0 长度流，用占位字节保证对象可创建
            cover = new byte[]{0};
        }
        minioOSSUtil.uploadToOSS(task.getThumbnailObjectName(),
                new ByteArrayInputStream(cover), cover.length, "image/jpeg");

        // 存在 90/270 旋转时交换宽高，保证前端封面比例正确
        Double rotationObj = videoMetadata.getRotation();
        int rotation = rotationObj != null ? rotationObj.intValue() : 0;
        boolean isRotated = (rotation == 90 || rotation == 270);
        if (isRotated) {
            fileEntity.setWidth(videoMetadata.getHeight());
            fileEntity.setHeight(videoMetadata.getWidth());
        } else {
            fileEntity.setWidth(videoMetadata.getWidth());
            fileEntity.setHeight(videoMetadata.getHeight());
        }

        fileEntity.setDateTimeOriginal(videoMetadata.getDateTimeOriginal());
        fileEntity.setMake(videoMetadata.getMake());
        fileEntity.setModel(videoMetadata.getModel());
        fileEntity.setLatitude(videoMetadata.getLatitude());
        fileEntity.setLatitudeRef(videoMetadata.getLatitudeRef());
        fileEntity.setLongitude(videoMetadata.getLongitude());
        fileEntity.setLongitudeRef(videoMetadata.getLongitudeRef());

        // 外键依赖 file 主表，实际插入延迟到 persistFile 之后
        videoMetadata.setFileId(fileEntity.getFileId());
        return videoMetadata;
    }

    private void persistFile(Long userId, UploadTask task, FileEntity fileEntity) {
        fileMapper.insert(fileEntity);

        Face face = new Face();
        face.setFileId(fileEntity.getFileId());
        face.setUserId(userId);
        // 显式标记为未处理，保证异步人脸消费者与定时任务都能扫到该记录
        face.setIsProcessed(false);
        faceMapper.insert(face);

        UserFileEntity userFile = new UserFileEntity();
        userFile.setUserId(userId);
        userFile.setFileId(fileEntity.getFileId());
        userFile.setIsDeleted(false);
        userFile.setUploadTime(LocalDateTime.now());
        userFileMapper.insert(userFile);

        if (task.getAlbumId() != null && task.getAlbumId() != -1) {
            albumService.addPictureToAlbum(Collections.singletonList(fileEntity.getFileId()),
                    task.getAlbumId(), userId);
        }
    }

    /**
     * 建立"已有文件 → 当前用户"的关联（user_file + 相册）。
     * 秒传命中与并发落库降级时复用，避免同一段关联逻辑散落多处。
     */
    private void ensureUserFileAssociation(Long userId, String fileId, Long albumId) {
        if (fileMapper.isUserHasFile(fileId, userId) == null) {
            UserFileEntity userFile = new UserFileEntity();
            userFile.setUserId(userId);
            userFile.setFileId(fileId);
            userFile.setIsDeleted(false);
            userFile.setUploadTime(LocalDateTime.now());
            userFileMapper.insert(userFile);
        }
        if (albumId != null && albumId != -1) {
            albumService.addPictureToAlbum(Collections.singletonList(fileId), albumId, userId);
        }
    }

    /**
     * 落库去重：以文件 MD5 为键加锁，串行化"检查文件是否已入库 + 落库"，
     * 避免并发 complete 对同一文件重复插入 file 行（同 MD5 对象名完全相同）。
     *
     * @return 返回非 null 表示文件已被并发请求落库，本次降级为秒传并返回该 fileId；
     *         返回 null 表示本次负责落库。
     */
    private String persistWithDedup(Long userId, UploadTask task, FileEntity fileEntity,
                                    ImageMetaData imageMeta, VideoMetaData videoMeta) {
        String lockKey = LOCK_KEY_PREFIX + task.getFileMd5();
        boolean locked = redisUtil.setIfAbsent(lockKey, "1", LOCK_SECONDS, TimeUnit.SECONDS);
        if (!locked) {
            // 已有并发请求持锁，等待其落库完成后再读取结果
            for (int i = 0; i < 30 && redisUtil.exists(lockKey); i++) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        try {
            String existingFileId = fileMapper.isFileExist(task.getFileMd5());
            if (existingFileId != null) {
                ensureUserFileAssociation(userId, existingFileId, task.getAlbumId());
                return existingFileId;
            }
            persistFile(userId, task, fileEntity);
            if (imageMeta != null) {
                imageMetadataMapper.insert(imageMeta);
            } else if (videoMeta != null) {
                videoMetaDataMapper.insert(videoMeta);
            }
            return null;
        } finally {
            if (locked) {
                redisUtil.delete(lockKey);
            }
        }
    }

    /**
     * 合并后校验整体 MD5 与声明是否一致。
     * 大文件跳过校验，避免合并接口长时间阻塞。
     */
    private void verifyMd5(UploadTask task) {
        if (!uploadConfig.isMd5VerifyEnabled() || task.getFileSize() > uploadConfig.getMd5VerifyMaxSize()) {
            return;
        }
        String actualMd5;
        try (InputStream in = minioOSSUtil.getObjectStream(task.getFileObjectName())) {
            actualMd5 = fileUtil.getMD5(in);
        } catch (IOException e) {
            throw new BusinessException(StatusCode.STORAGE_ERROR, "读取上传文件失败: " + e.getMessage());
        }
        if (!task.getFileMd5().equalsIgnoreCase(actualMd5)) {
            throw new BusinessException(StatusCode.MD5_MISMATCH, "文件校验失败，内容可能已损坏，请重新上传");
        }
    }

    private byte[] compressImage(byte[] source) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(source))
                    .size(uploadConfig.getThumbnailMaxSize(), uploadConfig.getThumbnailMaxSize())
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality((float) uploadConfig.getThumbnailQuality())
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "缩略图生成失败");
        }
    }

    private List<Integer> listUploadedChunkIndexes(UploadTask task) {
        List<Part> parts = minioMultipartUtil.listParts(task.getFileObjectName(), task.getMinioUploadId(), task.getTotalChunks());
        List<Integer> indexes = new ArrayList<>(parts.size());
        for (Part part : parts) {
            indexes.add(part.partNumber() - 1);
        }
        indexes.sort(Comparator.naturalOrder());
        return indexes;
    }

    // ---------- 任务存储 ----------

    private UploadTask requireTask(Long userId, String uploadId) {
        if (!StringUtils.hasText(uploadId)) {
            throw new BusinessException(StatusCode.UPLOAD_PARAM_MISSING, "缺少 uploadId");
        }
        UploadTask task = loadTask(uploadId);
        if (task == null) {
            throw new BusinessException(StatusCode.UPLOAD_TASK_NOT_FOUND, "上传任务不存在或已过期，请重新上传");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(StatusCode.NO_AUTH_ERROR, "无权操作该上传任务");
        }
        return task;
    }

    private UploadTask loadTask(String uploadId) {
        Object value = redisUtil.get(TASK_KEY_PREFIX + uploadId);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(String.valueOf(value), UploadTask.class);
        } catch (Exception e) {
            log.warn("上传任务反序列化失败: {}", uploadId, e);
            return null;
        }
    }

    private void saveTask(UploadTask task) {
        try {
            redisUtil.set(TASK_KEY_PREFIX + task.getUploadId(),
                    objectMapper.writeValueAsString(task),
                    uploadConfig.getTaskTtlHours(), TimeUnit.HOURS);
        } catch (Exception e) {
            throw new BusinessException(StatusCode.SYSTEM_ERROR, "保存上传任务失败: " + e.getMessage());
        }
    }

    private void touchTask(UploadTask task) {
        try {
            redisUtil.expire(TASK_KEY_PREFIX + task.getUploadId(),
                    uploadConfig.getTaskTtlHours(), TimeUnit.HOURS);
            redisUtil.expire(MD5_KEY_PREFIX + task.getFileMd5(),
                    uploadConfig.getTaskTtlHours(), TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("上传任务续期失败: {}", task.getUploadId(), e);
        }
    }

    private void clearTask(UploadTask task) {
        redisUtil.delete(TASK_KEY_PREFIX + task.getUploadId());
        redisUtil.sRemove(USER_TASK_KEY_PREFIX + task.getUserId(), task.getUploadId());
        redisUtil.hdel(ALIVE_TASK_KEY, task.getUploadId());
        redisUtil.delete(CHUNK_SET_KEY_PREFIX + task.getUploadId());
        Object cachedUploadId = redisUtil.get(MD5_KEY_PREFIX + task.getFileMd5());
        if (cachedUploadId != null && task.getUploadId().equals(String.valueOf(cachedUploadId))) {
            redisUtil.delete(MD5_KEY_PREFIX + task.getFileMd5());
        }
    }

    /**
     * 限制单用户并发上传任务数，并顺带清理集合中已失效的任务记录，
     * 避免 Set 长期续期后堆积过期 uploadId 导致误判。
     */
    private void checkConcurrentLimit(Long userId) {
        String key = USER_TASK_KEY_PREFIX + userId;
        Set<Object> taskIds = redisUtil.sGet(key);
        if (taskIds != null) {
            for (Object id : taskIds) {
                if (!redisUtil.exists(TASK_KEY_PREFIX + id)) {
                    redisUtil.sRemove(key, id);
                }
            }
        }
        if (redisUtil.sSize(key) >= uploadConfig.getMaxConcurrentTasks()) {
            throw new BusinessException(StatusCode.UPLOAD_CONCURRENT_LIMIT,
                    "同时上传的文件数量过多（上限 " + uploadConfig.getMaxConcurrentTasks() + " 个），请稍后再试");
        }
    }

    private void registerUserTask(Long userId, String uploadId) {
        String key = USER_TASK_KEY_PREFIX + userId;
        redisUtil.sAdd(key, uploadId);
        redisUtil.expire(key, uploadConfig.getTaskTtlHours(), TimeUnit.HOURS);
    }

    private void registerAliveTask(UploadTask task) {
        redisUtil.hset(ALIVE_TASK_KEY, task.getUploadId(),
                task.getFileObjectName() + ALIVE_VALUE_SEPARATOR + task.getMinioUploadId());
    }

    private void safeDelete(String objectName) {
        try {
            minioOSSUtil.delete(objectName);
        } catch (Exception e) {
            log.warn("清理对象失败（可忽略）: {}", objectName, e);
        }
    }

    // ---------- 工具方法 ----------

    private LocalDateTime parseLastModified(String lastModified) {
        if (StringUtils.hasText(lastModified)) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastModified)),
                        ZoneId.systemDefault());
            } catch (NumberFormatException e) {
                log.warn("文件修改时间格式错误: {}", lastModified);
            }
        }
        return LocalDateTime.now();
    }

    private String resolveContentType(String suffix, String category) {
        return switch (suffix) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "jpg", "jpeg" -> "image/jpeg";
            case "mp4" -> "video/mp4";
            case "mkv" -> "video/x-matroska";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "rmvb" -> "application/vnd.rn-realmedia-vbr";
            default -> "image".equals(category) ? "image/jpeg" : "video/mp4";
        };
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return (bytes / (1024L * 1024 * 1024)) + "GB";
        }
        if (bytes >= 1024L * 1024) {
            return (bytes / (1024L * 1024)) + "MB";
        }
        return (bytes / 1024) + "KB";
    }
}
