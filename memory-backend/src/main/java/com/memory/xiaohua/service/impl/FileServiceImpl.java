package com.memory.xiaohua.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.*;
import com.memory.xiaohua.model.dto.DownLoadInfoDTO;
import com.memory.xiaohua.model.entity.*;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.entity.FileInfo;
import com.memory.xiaohua.model.vo.entity.FileMetaDataVO;
import com.memory.xiaohua.model.vo.entity.ShareFileVO;
import com.memory.xiaohua.service.AlbumService;
import com.memory.xiaohua.service.FileService;
import com.memory.xiaohua.service.RecordService;
import com.memory.xiaohua.service.UserService;
import com.memory.xiaohua.utils.RedisKeyConstants;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.file.FileUtil;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import com.memory.xiaohua.utils.metaData.ImageMetadataParserUtil;
import com.memory.xiaohua.utils.metaData.VideoMetadataParserUtil;
import com.memory.xiaohua.utils.picture.OpenCVPictureUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import static cn.dev33.satoken.SaManager.log;


/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/20,0:22
 */
@Service
public class FileServiceImpl implements FileService {

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
    private AlbumService albumService;

    @Resource
    private ImageMetaDataMapper imageMetadataMapper;

    @Resource
    private VideoMetaDataMapper videoMetaDataMapper;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private UserService userService;

    @Resource
    private PictureTagMapper pictureTagMapper;

    @Resource
    private FaceMapper faceMapper;

    @Resource
    private RecordService recordService;

    @Resource
    private OpenCVPictureUtil openCVPictureUtil;

    @Resource
    private SimilarPictureMapper similarPictureMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean uploadImageFile(Long userId, MultipartFile multipartFile, String fileSuffix, LocalDateTime lastModifiedTime, Long albumId) {

        //用于计算MD5
        InputStream originalInputStream1;
        //用于上传
        InputStream originalInputStream2;
        //压缩图片上传
        InputStream thumbnailInputStream;
        byte[] fileBytes;
        try {
            originalInputStream1 = multipartFile.getInputStream();
            originalInputStream2 = multipartFile.getInputStream();
            thumbnailInputStream = multipartFile.getInputStream();
            fileBytes = multipartFile.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("文件流错误: " + e.getMessage(), e);
        }
        //计算MD5
        String fileMd5 = fileUtil.getMD5(originalInputStream1);
        // 获取图片元数据
        ImageMetaData imageMetadata = new ImageMetaData();
        try {
            imageMetadata = imageMetadataParserUtil.getImageMetadata(multipartFile);
        } catch (Exception e) {
            log.info(String.valueOf(e));
        }

        LocalDateTime dateTimeOriginal = imageMetadata.getDateTimeOriginal();

        if (dateTimeOriginal == null) {
            imageMetadata.setDateTimeOriginal(lastModifiedTime);
        }
        long thumbnailFileSize;
        //大于500kb进行压缩
        if (multipartFile.getSize() > 500 * 1000) {
            // 压缩图片
            ByteArrayOutputStream thumbnailArrayOutputStream = fileUtil.outputQuality(fileBytes);
            byte[] thumbnailByteArray = thumbnailArrayOutputStream.toByteArray();
            thumbnailFileSize = thumbnailByteArray.length;
            thumbnailInputStream = new ByteArrayInputStream(thumbnailByteArray);
        } else {
            thumbnailFileSize = multipartFile.getSize();
        }


        LocalDateTime uploadTime = LocalDateTime.now();
        LocalDate uploadDate = uploadTime.toLocalDate();
        //替换为合法 Minio ObjectName
        String time = uploadDate.toString();

        String fileObjectName = "file/" + time + "/" + fileMd5 + "." + fileSuffix;
        String thumbnailObjectName = "thumbnail/" + time + "/" + fileMd5 + "." + fileSuffix;
        //上传原文件
        minioOSSUtil.uploadToOSS(fileObjectName, originalInputStream2, multipartFile.getSize(), multipartFile.getContentType());
        //上传缩略图
        minioOSSUtil.uploadToOSS(thumbnailObjectName, thumbnailInputStream, thumbnailFileSize, multipartFile.getContentType());


        String fileId = UUID.randomUUID().toString().replace("-", "");
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileId(fileId);
        fileEntity.setOriginFileName(multipartFile.getOriginalFilename());
        fileEntity.setSize(multipartFile.getSize());
        fileEntity.setLastModifiedTime(lastModifiedTime);
        fileEntity.setContentType(multipartFile.getContentType());
        //类别
        fileEntity.setCategory("image");
        String fileUrl = minioOSSUtil.getFileUrl(fileObjectName);
        String thumbnailFileUrl = minioOSSUtil.getFileUrl(thumbnailObjectName);
        fileEntity.setFileUrl(fileUrl);
        fileEntity.setThumbnailUrl(thumbnailFileUrl);
        fileEntity.setFileObjectName(fileObjectName);
        fileEntity.setThumbnailObjectName(thumbnailObjectName);
        fileEntity.setMd5(fileMd5);

        fileEntity.setDateTimeOriginal(imageMetadata.getDateTimeOriginal());
        fileEntity.setWidth(imageMetadata.getWidth());
        fileEntity.setHeight(imageMetadata.getHeight());
        fileEntity.setMake(imageMetadata.getMake());
        fileEntity.setModel(imageMetadata.getModel());
        fileEntity.setLatitude(imageMetadata.getLatitude());
        fileEntity.setLatitudeRef(imageMetadata.getLatitudeRef());
        fileEntity.setLongitude(imageMetadata.getLongitude());
        fileEntity.setLongitudeRef(imageMetadata.getLongitudeRef());
        //插入数据库
        fileMapper.insert(fileEntity);
        Face face = new Face();
        face.setFileId(fileId);
        face.setUserId(userId);
        faceMapper.insert(face);
        UserFileEntity userFileEntity = new UserFileEntity();
        userFileEntity.setUserId(userId);
        userFileEntity.setFileId(fileId);
        userFileEntity.setIsDeleted(false);
        userFileEntity.setUploadTime(uploadTime);
        userFileMapper.insert(userFileEntity);
        imageMetadata.setFileId(fileId);

        //-1表示不上传到相册
        if (albumId != -1) {
            //加入相册
            List<String> list = new ArrayList<>();
            list.add(fileId);
            albumService.addPictureToAlbum(list, albumId, userId);

        }
        imageMetadataMapper.insert(imageMetadata);

        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean uploadVideoFile(Long userId, MultipartFile multipartFile, String fileSuffix, LocalDateTime lastModifiedTime, Long albumId) {

        //用于计算MD5
        InputStream originalInputStream1;
        //用于上传
        InputStream originalInputStream2;

        try {
            originalInputStream1 = multipartFile.getInputStream();
            originalInputStream2 = multipartFile.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("文件流错误: " + e.getMessage(), e);
        }
        //计算MD5
        String fileMd5 = fileUtil.getMD5(originalInputStream1);
        // 获取视频元数据
        VideoMetaData videoMetadata = videoMetadataParserUtil.GetVideoMetadata(multipartFile);

        LocalDateTime dateTimeOriginal = videoMetadata.getDateTimeOriginal();
        if (dateTimeOriginal == null) {
            videoMetadata.setDateTimeOriginal(lastModifiedTime);
        }

        // 获取视频封面
        byte[] thumbnailByteArray = videoMetadataParserUtil.extractCover(multipartFile);
        // 2.3 修复：extractCover 可能返回 null（视频无画面），直接使用会 NPE
        if (thumbnailByteArray == null || thumbnailByteArray.length == 0) {
            thumbnailByteArray = new byte[]{0};  // 占位字节，MinIO 不接受 0 长度流
        }


        LocalDateTime uploadTime = LocalDateTime.now();

        LocalDate uploadDate = uploadTime.toLocalDate();
        //替换为合法 Minio ObjectName
        String time = uploadDate.toString();

        String fileObjectName = "file/" + time + "/" + fileMd5 + "." + fileSuffix;
        String thumbnailObjectName = "thumbnail/" + time + "/" + fileMd5 + "." + fileSuffix;

        //上传原文件
        minioOSSUtil.uploadToOSS(fileObjectName, originalInputStream2, multipartFile.getSize(), multipartFile.getContentType());
        
        //上传视频封面
        minioOSSUtil.uploadToOSS(thumbnailObjectName, new ByteArrayInputStream(thumbnailByteArray), thumbnailByteArray.length, "image/jpeg");

        //上传时间
        String fileId = UUID.randomUUID().toString().replace("-", "");
        FileEntity fileEntity = new FileEntity();
        fileEntity.setWidth(videoMetadata.getWidth());
        fileEntity.setHeight(videoMetadata.getHeight());

        // 2.3 修复：getRotation 可能为 null（元数据解析失败），拆箱 NPE
        Double rotationObj = videoMetadata.getRotation();
        int rotation = rotationObj != null ? rotationObj.intValue() : 0;
        //为解决视频前端封面显示问题，需要正确设置width、height，如果有旋转，就跳转宽高像素顺序
        boolean isRotation = (rotation == 90 || rotation == 270);
        if(isRotation){
            fileEntity.setWidth(videoMetadata.getHeight());
            fileEntity.setHeight(videoMetadata.getWidth());
        }
        fileEntity.setFileId(fileId);

        fileEntity.setOriginFileName(multipartFile.getOriginalFilename());
        fileEntity.setSize(multipartFile.getSize());

        fileEntity.setLastModifiedTime(lastModifiedTime);
        fileEntity.setContentType(multipartFile.getContentType());
        //类别
        fileEntity.setCategory("video");
        String fileUrl = minioOSSUtil.getFileUrl(fileObjectName);
        String thumbnailFileUrl = minioOSSUtil.getFileUrl(thumbnailObjectName);
        fileEntity.setFileUrl(fileUrl);
        fileEntity.setThumbnailUrl(thumbnailFileUrl);
        fileEntity.setFileObjectName(fileObjectName);
        fileEntity.setThumbnailObjectName(thumbnailObjectName);
        fileEntity.setMd5(fileMd5);


        fileEntity.setMake(videoMetadata.getMake());
        fileEntity.setModel(videoMetadata.getModel());
        fileEntity.setDateTimeOriginal(videoMetadata.getDateTimeOriginal());
        fileEntity.setLatitude(videoMetadata.getLatitude());
        fileEntity.setLatitudeRef(videoMetadata.getLatitudeRef());
        fileEntity.setLongitude(videoMetadata.getLongitude());
        fileEntity.setLongitudeRef(videoMetadata.getLongitudeRef());
        //插入数据库
        fileMapper.insert(fileEntity);
        Face face = new Face();
        face.setFileId(fileId);
        face.setUserId(userId);
        faceMapper.insert(face);
        UserFileEntity userFileEntity = new UserFileEntity();
        userFileEntity.setUserId(userId);
        userFileEntity.setFileId(fileId);
        userFileEntity.setIsDeleted(false);
        userFileEntity.setUploadTime(uploadTime);
        userFileMapper.insert(userFileEntity);
        videoMetadata.setFileId(fileId);
        //-1表示不上传到相册
        if (albumId != -1) {
            List<String> list = new ArrayList<>();
            list.add(fileId);
            albumService.addPictureToAlbum(list, albumId, userId);
        }

        videoMetaDataMapper.insert(videoMetadata);


        return true;
    }

    @Override
    public Boolean checkIsUpload(Long userId, Long albumId, String fileMd5) {
        String fileId = fileMapper.isFileExist(fileMd5);
        //数据库没有该文件，返回false
        if (fileId == null) return false;

        String userHasFile = fileMapper.isUserHasFile(fileId, userId);
        //数据库有该文件，但是用户没有该文件，直接将该文件记录给用户
        if (userHasFile == null) {
            UserFileEntity userFileEntity = new UserFileEntity();
            userFileEntity.setUserId(userId);
            userFileEntity.setFileId(fileId);
            userFileEntity.setIsDeleted(false);
            userFileEntity.setUploadTime(LocalDateTime.now());
            userFileMapper.insert(userFileEntity);
        }
        //文件已上传，并且还指定
        if (albumId != -1) {
            List<String> list = new ArrayList<>();
            list.add(fileId);
            albumService.addPictureToAlbum(list, albumId, userId);
        }
        return true;
    }

    @Override
    public void saveSharePicture(Long userId, Long albumId, List<String> fileIds, String shareToken) {
        // 越权修复：必须携带有效分享凭证，且保存的文件必须属于该分享，防止任意 fileId 被加入账户
        if (shareToken == null || shareToken.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "缺少分享凭证!");
        }
        Map<String, String> shareMap = redisUtil.hgetAllAsString(RedisKeyConstants.SHARE_LINK + shareToken);
        if (shareMap == null || shareMap.isEmpty()) {
            throw new BusinessException(StatusCode.NOT_FOUND_ERROR, "分享链接已过期或不存在");
        }
        List<String> sharedFileIds = JSON.parseArray(shareMap.get("fileIds"), String.class);
        if (sharedFileIds == null || !sharedFileIds.containsAll(fileIds)) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "存在不属于该分享的文件!");
        }

        for (String fileId : fileIds) {
            String userHasFile = fileMapper.isUserHasFile(fileId, userId);
            //用户没有这些文件
            if (userHasFile == null) {
                UserFileEntity userFileEntity = new UserFileEntity();
                userFileEntity.setUserId(userId);
                userFileEntity.setFileId(fileId);
                userFileEntity.setIsDeleted(false);
                userFileEntity.setUploadTime(LocalDateTime.now());
                userFileMapper.insert(userFileEntity);
            }
        }
        //如果要求保存到相册
        if (albumId != -1) {
            albumService.addPictureToAlbum(fileIds, albumId, userId);
        }

    }


    public Page<FileInfoListVO> getFileInfoList(Integer current, Integer size, String orderType, String orderKeyword, String imageTypeText, String locationLevel, String locationValue, Long userId, Long albumId, Boolean isDeleted) {
        Page<FileInfoListVO> page = new Page<>(current, size);
        List<FileInfoListVO> fileInfoListVOPage = fileMapper.getFileInfoList(page, orderType, orderKeyword, imageTypeText, locationLevel, locationValue, userId, albumId, false);
        page.setRecords(fileInfoListVOPage);
        return page;
    }

    @Override
    public Page<FileInfoListVO> getTagFileInfo(Integer current, Integer size, Long userId, String orderType, String orderKeyword, String imageTypeText, String tagName) {
        Page<FileInfoListVO> page = new Page<>(current, size);
        List<FileInfoListVO> fileInfoListVOPage = fileMapper.getTagFileInfo(page, userId, orderType, orderKeyword, imageTypeText, tagName, false);
        page.setRecords(fileInfoListVOPage);
        return page;
    }

    @Override
    public List<FileInfoListVO> getSimilarFileList(String imageTypeText,Long userId) {


        return fileMapper.selectAllSimilarPicture(userId, imageTypeText);
    }

    @Override
    public void createSimilarFileList(Double similarity, Integer size, Long userId) {
        QueryWrapper<SimilarPicture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        similarPictureMapper.delete(queryWrapper);
        List<FileInfo> fileInfos = fileMapper.selectAllFileByUserId(userId,size);
        Map<Integer, List<FileInfo>> picturesMap = new HashMap<>();
        int groupId = 0;  // 分组ID自增器
        for (FileInfo currentFile : fileInfos) {
            try {
                // 1. 获取缩略图字节数据
                String thumbnailObjectName = currentFile.getThumbnailObjectName();
                byte[] currentBytes = minioOSSUtil.getFileBytes(thumbnailObjectName);

                boolean isGrouped = false;

                // 2. 遍历已有分组寻找相似组
                for (Map.Entry<Integer, List<FileInfo>> entry : picturesMap.entrySet()) {
                    // 取组内第一个文件作为代表进行比较
                    FileInfo sampleFile = entry.getValue().get(0);
                    byte[] sampleBytes = minioOSSUtil.getFileBytes(sampleFile.getThumbnailObjectName());
                    if (sampleBytes == null) continue;

                    // 3. 使用OpenCV计算相似度
                    boolean samePicture = openCVPictureUtil.isSamePicture(similarity,currentBytes, sampleBytes);
                    if (samePicture) {
                        entry.getValue().add(currentFile);
                        isGrouped = true;
                        break;
                    }
                }
                // 4. 若无相似组则创建新组
                if (!isGrouped) {
                    List<FileInfo> newGroup = new ArrayList<>();
                    newGroup.add(currentFile);
                    picturesMap.put(groupId++, newGroup);
                }
            } catch (Exception e) {
                // 处理文件读取异常
                log.error("处理文件失败: {}", currentFile.getFileId(), e);
            }
        }
        for (Map.Entry<Integer, List<FileInfo>> entry : picturesMap.entrySet()) {
            if(entry.getValue().size()<2){
                continue;
            }
            List<FileInfo> list = entry.getValue();
            FileInfo fileInfo = list.get(0);
            list.remove(0);
            SimilarPicture mainPicture = new SimilarPicture();
            mainPicture.setFileId(fileInfo.getFileId());
            mainPicture.setUserId(userId);
            String id = UUID.randomUUID().toString().replace("-", "");
            mainPicture.setSimilarId(id);
            similarPictureMapper.insert(mainPicture);
            for (FileInfo file : list) {
                SimilarPicture similar = new SimilarPicture();
                similar.setSimilarId(id);
                similar.setFileId(file.getFileId());
                similar.setUserId(userId);
                similarPictureMapper.insert(similar);
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setIsDeleted(List<String> fileIds, boolean isDeleted, Long userId) {
        int update = fileMapper.setIsDeletedByFileIds(fileIds, isDeleted, userId);
        //从相册里删除
        albumService.removePictureFromAlbum(fileIds, null, userId);
        return update > 0;
    }

    @Override
    public void downloadFileByIds(HttpServletResponse response, List<String> fileIds, Long userId) {
        // 1. 获取文件列表
        List<FileEntity> fileEntities = fileMapper.selectFileByIds(fileIds, userId);
        for (FileEntity fileEntity : fileEntities) {
            minioOSSUtil.download(response, fileEntity);
        }
    }

    @Override
    public String getDownloadToken(List<String> fileIds, Long userId) {
        String Token = (UUID.randomUUID().toString().replace("-", ""));
        // 存储到临时缓存（Sa-Token内置缓存API）
        DownLoadInfoDTO downLoadInfoDTO = new DownLoadInfoDTO();
        downLoadInfoDTO.setFileIds(fileIds);
        downLoadInfoDTO.setUserId(userId);
        redisUtil.set(RedisKeyConstants.TEMP_DOWNLOAD + Token, downLoadInfoDTO, 60, TimeUnit.MINUTES);
        return Token;
    }


    /**
     * 获取下载相册的token
     *
     * @param albumId
     * @param userId
     * @return
     */
    @Override
    public String getDownloadAlbumToken(Long albumId, Long userId) {
        String Token = (UUID.randomUUID().toString().replace("-", ""));
        // 存储到临时缓存（Sa-Token内置缓存API）
        DownLoadInfoDTO downLoadInfoDTO = new DownLoadInfoDTO();
        downLoadInfoDTO.setAlbumId(albumId);
        downLoadInfoDTO.setUserId(userId);
        redisUtil.set(RedisKeyConstants.TEMP_DOWNLOAD + Token, downLoadInfoDTO, 60, TimeUnit.MINUTES);
        return Token;
    }

    @Override
    public void downloadAlbumByToken(HttpServletRequest request, HttpServletResponse response, Long albumId, Long userId) {
        List<String> fileIds = fileMapper.selectFileIdByAlbumId(albumId, userId);
        if (fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "相册没有文件!");
        }
        Album album = albumService.getById(albumId);
        // 2. 动态生成文件名
        String downloadZipName = "Memory_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH时mm分")) +
                "_" + album.getAlbumName() + fileIds.size() + "项.zip";
        this.downloadZipFileByIds(response, fileIds, userId, downloadZipName);
        //日志记录
        recordService.createRecordLog("下载相册:" + album.getAlbumName(), fileIds.size(), userId, request);
    }


    @Override
    public void downloadZipFileByIds(HttpServletResponse response, List<String> fileIds, Long userId, String downloadZipName) {
        try {
            List<FileEntity> fileEntities = fileMapper.selectFileByIds(fileIds, userId);
            // 1. 设置响应头（不设置Content-Length）
            response.setContentType("application/zip");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            long totalSize = minioOSSUtil.calculateZipTotalSize(fileEntities);
            response.setContentLengthLong(totalSize);


            String encodedFileName = URLEncoder.encode(downloadZipName, StandardCharsets.UTF_8).replace("+", "%20");

            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);


            // 3. 流式生成ZIP并传输
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                Map<String, Integer> fileNameCounter = new HashMap<>();
                for (FileEntity fileEntity : fileEntities) {
                    minioOSSUtil.downloadFileToZip(zos, fileEntity, fileNameCounter);
                    // 强制刷新输出流
                    zos.flush();
                    response.flushBuffer(); // 关键：确保数据实时推送
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetaDataVO selectFileMetaDataById(String fileId, Long userId) {
        return fileMapper.selectFileMetaDataById(fileId, userId);
    }

    @Override
    public String createShareUrl(List<String> fileIds, Long userId, Integer shareDay) {
        // 越权修复：校验所有文件均归属当前用户，防止用任意 fileId 生成分享链接
        for (String fileId : fileIds) {
            if (fileMapper.isUserHasFile(fileId, userId) == null) {
                throw new BusinessException(StatusCode.PARAMS_ERROR, "存在无权分享的文件!");
            }
        }

        String shareKey = UUID.randomUUID().toString().replace("-", "");
        // 构造Redis存储数据
        String redisKey = RedisKeyConstants.SHARE_LINK + shareKey;
        Map<String, String> shareData = new HashMap<>();
        shareData.put("userId", userId.toString());
        shareData.put("fileIds", JSON.toJSONString(fileIds));
        shareData.put("visitCount", "0");

        // 一次性设置Hash和过期时间
        redisUtil.hmset(redisKey, shareData, shareDay * 24, TimeUnit.HOURS);

        return shareKey;
    }


    @Override
    public String createAlbumShareUrl(List<Long> albumIds, Long userId, Integer shareDay) {
        List<String> fileIds = new ArrayList<>();
        for (Long albumId : albumIds) {
            List<String> res = fileMapper.selectFileIdByAlbumId(albumId, userId);
            fileIds.addAll(res);
        }

        return this.createShareUrl(fileIds, userId, shareDay);
    }

    @Override
    public ShareFileVO getShareInfo(String shareKey) {
        shareKey = RedisKeyConstants.SHARE_LINK + shareKey;
        // 2.6 修复：先判空再取 expire（原顺序反了，过期时 getExpire 返回负数，localDateTime 计算无意义）
        Map<String, String> shareMap = redisUtil.hgetAllAsString(shareKey);
        if (shareMap == null || shareMap.isEmpty()) {
            throw new BusinessException(StatusCode.NOT_FOUND_ERROR, "分享链接已过期或不存在");
        }
        Long expireMillis = redisUtil.getExpire(shareKey);
        LocalDateTime expireTime = LocalDateTime.now().plus(expireMillis, ChronoUnit.MILLIS);
        Long sharerId = Long.parseLong((String) shareMap.get("userId"));
        List<String> fileIds = JSON.parseArray((String) shareMap.get("fileIds"), String.class);
        Long visitCount = Long.parseLong((String) shareMap.get("visitCount"));
        // 2.6 修复：访问计数自增并写回（原功能未实现，永远是 0）
        redisUtil.hset(shareKey, "visitCount", String.valueOf(visitCount + 1));


        // 3. 获取用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", sharerId);
        User user = userService.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分享用户不存在");
        }
        List<FileInfoListVO> fileInfoListVOS = fileMapper.selectFileInfoListByIds(fileIds);
        ShareFileVO vo = new ShareFileVO();
        vo.setExpireTime(expireTime);
        vo.setFileInfoList(fileInfoListVOS);
        vo.setSharePersonName(user.getUserName());
        vo.setSharePersonAvatar(user.getAvatarUrl());
        vo.setVisitCount(visitCount);
        return vo;
    }

    @Override
    public boolean addTag(List<String> fileIds,String imageType, String tagName, Long userId) {
        for (String fileId : fileIds) {
            PictureTag pictureTag = new PictureTag();
            String id = fileMapper.isUserHasFile(fileId, userId);
            if (id == null) continue;
            pictureTag.setFileId(id);
            pictureTag.setImageType(imageType);
            pictureTag.setTagName(tagName);
            QueryWrapper<PictureTag> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", id);

            queryWrapper.eq("tag_name", tagName);
            PictureTag res = pictureTagMapper.selectOne(queryWrapper);
            if (res != null) {
                continue;
            }
            pictureTagMapper.insert(pictureTag);
        }
        return true;
    }

    @Override
    public boolean removeTag(List<String> fileIds, String tagName, Long userId) {
        for (String fileId : fileIds) {
            String id = fileMapper.isUserHasFile(fileId, userId);
            if (id == null) continue;
            QueryWrapper<PictureTag> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", id);
            queryWrapper.eq("tag_name", tagName);
            pictureTagMapper.delete(queryWrapper);
        }

        return true;
    }

    @Override
    public List<String> selectTagByFileId(String fileId, Long userId) {
        return pictureTagMapper.selectTagsByFileId(fileId, userId);
    }

    @Override
    public List<String> selectAllModels(Long userId) {
        return fileMapper.selectAllModels(userId);
    }

    /**
     * 由于minio获取的url最长为7天，所以需要定时更换数据库Url
     */
    @Override
    public void cronUpdateFileUrl() {
        Wrapper<FileEntity> queryWrapper1 = new QueryWrapper<>();
        //更新照片链接
        List<FileEntity> fileEntities = fileMapper.selectList(queryWrapper1);
        for (FileEntity fileEntity : fileEntities) {
            String fileId = fileEntity.getFileId();
            String fileObjectName = fileEntity.getFileObjectName();
            String thumbnailObjectName = fileEntity.getThumbnailObjectName();
            String fileUrl = minioOSSUtil.getFileUrl(fileObjectName);
            String thumbnailUrl = minioOSSUtil.getFileUrl(thumbnailObjectName);
            UpdateWrapper<FileEntity> updateWrapper1 = new UpdateWrapper<>();
            updateWrapper1.eq("file_id", fileId);
            updateWrapper1.set("file_url", fileUrl);
            updateWrapper1.set("thumbnail_url", thumbnailUrl);
            fileMapper.update(updateWrapper1);
        }
        //更新人物照片链接
        Wrapper<Face> queryWrapper2 = new QueryWrapper<>();
        List<Face> faces = faceMapper.selectList(queryWrapper2);

        for (Face face : faces) {
            if (face.getPersonCoverUrl() == null) {
                continue;
            }
            Long faceId = face.getFaceId();
            String personObjectName = face.getPersonObjectName();
            UpdateWrapper<Face> updateWrapper2 = new UpdateWrapper<>();
            String fileUrl = minioOSSUtil.getFileUrl(personObjectName);
            updateWrapper2.eq("face_id", faceId);
            updateWrapper2.set("person_cover_url", fileUrl);
            faceMapper.update(updateWrapper2);
        }
        //更新用户头像链接
        Wrapper<User> queryWrapper3 = new UpdateWrapper<>();
        BaseMapper<User> userMapper = userService.getBaseMapper();
        List<User> users = userMapper.selectList(queryWrapper3);
        for (User user : users) {
            if (user.getAvatarUrl() == null) {
                continue;
            }
            Long userId = user.getUserId();
            String avatarObjectName = user.getAvatarObjectName();
            UpdateWrapper<User> updateWrapper3 = new UpdateWrapper<>();
            String fileUrl = minioOSSUtil.getFileUrl(avatarObjectName);
            updateWrapper3.eq("id", userId);
            updateWrapper3.set("avatar_url", fileUrl);
            userMapper.update(updateWrapper3);
        }
    }
}
