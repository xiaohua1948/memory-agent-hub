package com.memory.xiaohua.utils.file;

import com.memory.xiaohua.config.MinIOConfig;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import io.minio.messages.Part;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MinIO 分片上传封装（基于 composeObject 的"分片即临时对象"方案）
 *
 * <p>MinIO 8.5.x 的底层 S3 multipart 方法（createMultipartUpload / uploadPart 等）为 protected，
 * 不对外暴露。因此这里采用官方推荐、且对服务端零本地磁盘占用的做法：</p>
 * <ol>
 *   <li>每个分片以独立临时对象上传到 {@code file/chunks/{uploadId}/{partNumber}}；</li>
 *   <li>合并阶段用 {@link ComposeObjectArgs} 由服务端把分片对象拼接成最终对象（零拷贝）；</li>
 *   <li>合并/中止后清理临时分片对象。</li>
 * </ol>
 *
 * <p>约束：composeObject 最多 10000 个源对象，单个源 ≤ 5GB；本项目分片默认 5MB，
 * 因此单文件上限约 50GB，远高于 2GB 业务限制，满足需求。</p>
 */
@Slf4j
@Component
public class MinioMultipartUtil {

    /** 分片临时对象前缀 */
    private static final String CHUNK_PREFIX = "file/chunks/";

    private static final Pattern CHUNK_NAME_PATTERN =
            Pattern.compile("^file/chunks/(.+)/(\\d+)$");

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinIOConfig configuration;

    /**
     * 创建分片上传任务，返回 uploadId（用于组织临时分片对象名）
     */
    public String createMultipartUpload(String objectName, String contentType) {
        // 无需提前在 MinIO 建任务，uploadId 仅用于命名空间隔离
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 上传单个分片：以临时对象形式落到 MinIO
     *
     * @param partNumber 分片编号，从 1 开始
     * @return 该分片的 ETag，合并时需要按序回传
     */
    public String uploadPart(String objectName, String uploadId, int partNumber,
                             InputStream inputStream, long size) {
        String chunkObject = chunkObjectName(uploadId, partNumber);
        try (inputStream) {
            minioClient.putObject(io.minio.PutObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(chunkObject)
                    .stream(inputStream, size, -1)
                    .build());
            // 取回该分片对象的 ETag 作为合并凭据
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(chunkObject)
                    .build());
            return stat.etag();
        } catch (Exception e) {
            log.error("上传分片失败: object={}, uploadId={}, part={}", objectName, uploadId, partNumber, e);
            throw new BusinessException(StatusCode.STORAGE_ERROR, "上传分片失败: " + e.getMessage());
        }
    }

    /**
     * 列出已上传的分片（通过 statObject 探测 1..totalChunks）
     *
     * @param expectedTotalChunks 已知总分片数，用于探测边界
     */
    public List<Part> listParts(String objectName, String uploadId, int expectedTotalChunks) {
        List<Part> result = new ArrayList<>();
        for (int i = 1; i <= expectedTotalChunks; i++) {
            String chunkObject = chunkObjectName(uploadId, i);
            try {
                StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(configuration.getBucketName())
                        .object(chunkObject)
                        .build());
                result.add(new Part(i, stat.etag()));
            } catch (Exception e) {
                if (!isNotFound(e)) {
                    log.error("列出分片失败: uploadId={}, part={}", uploadId, i, e);
                    throw new BusinessException(StatusCode.STORAGE_ERROR, "查询分片状态失败: " + e.getMessage());
                }
                // 对象不存在 = 该分片尚未上传，跳过
            }
        }
        result.sort(Comparator.comparingInt(Part::partNumber));
        return result;
    }

    /**
     * 判断是否为"对象不存在"类异常，区分于存储服务故障/鉴权失败等真实错误。
     */
    private boolean isNotFound(Exception e) {
        if (e instanceof ErrorResponseException error) {
            String code = error.errorResponse().code();
            return "NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NotFound".equals(code);
        }
        return false;
    }

    /**
     * 合并分片：由 MinIO 服务端 composeObject 拼接，不产生本地 IO
     *
     * @param parts 按 partNumber 升序排列的全量分片（含 ETag）
     */
    public void completeMultipart(String objectName, String uploadId, Part[] parts) {
        if (parts == null || parts.length == 0) {
            throw new BusinessException(StatusCode.UPLOAD_MERGE_FAILED, "没有可合并的分片");
        }
        List<ComposeSource> sources = new ArrayList<>(parts.length);
        for (Part part : parts) {
            String chunkObject = chunkObjectName(uploadId, part.partNumber());
            // 不指定 length：MinIO 默认使用整个分片对象拼接，避免 messages.Part 无 partSize 字段导致的 NPE
            sources.add(ComposeSource.builder()
                    .bucket(configuration.getBucketName())
                    .object(chunkObject)
                    .build());
        }
        try {
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .sources(sources)
                    .build());
            // 合并成功后清理临时分片
            deleteChunks(uploadId);
        } catch (Exception e) {
            log.error("合并分片失败: object={}, uploadId={}", objectName, uploadId, e);
            throw new BusinessException(StatusCode.UPLOAD_MERGE_FAILED, "文件合并失败: " + e.getMessage());
        }
    }

    /**
     * 中止上传，清理 MinIO 上已收到的分片对象
     */
    public void abortMultipart(String objectName, String uploadId) {
        deleteChunks(uploadId);
    }

    /**
     * 列出并删除某 uploadId 下的全部临时分片对象
     */
    private void deleteChunks(String uploadId) {
        String prefix = CHUNK_PREFIX + uploadId + "/";
        try {
            List<DeleteObject> toDelete = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(configuration.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                toDelete.add(new DeleteObject(item.objectName()));
            }
            if (!toDelete.isEmpty()) {
                Iterable<Result<io.minio.messages.DeleteError>> deleted =
                        minioClient.removeObjects(RemoveObjectsArgs.builder()
                                .bucket(configuration.getBucketName())
                                .objects(toDelete)
                                .build());
                // 消费迭代器以触发实际删除
                for (Result<io.minio.messages.DeleteError> ignored : deleted) {
                    // no-op
                }
            }
        } catch (Exception e) {
            log.warn("清理分片对象失败（可忽略）: uploadId={}", uploadId, e);
        }
    }

    private String chunkObjectName(String uploadId, int partNumber) {
        return CHUNK_PREFIX + uploadId + "/" + partNumber;
    }

    /**
     * 供 {@link UploadServiceImpl} 的存活任务清理使用（保留以兼容调用方）
     */
    public static boolean isChunkObject(String objectName) {
        Matcher matcher = CHUNK_NAME_PATTERN.matcher(objectName);
        return matcher.matches();
    }
}
