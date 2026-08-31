package com.memory.xiaohua.model.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传任务状态
 *
 * <p>仅存在于 Redis（非数据库表），用于串联 pre-check / chunk / complete 三个阶段。
 * 已上传分片清单以 MinIO 的 listMultipart 为准，避免 Redis 与对象存储状态不一致。</p>
 */
@Data
public class UploadTask implements Serializable {

    /** 业务上传任务 ID */
    private String uploadId;

    private Long userId;

    /** 整个文件的 MD5（秒传与去重的依据） */
    private String fileMd5;

    /** 原始文件名 */
    private String originFileName;

    /** 文件后缀（不含点，统一小写） */
    private String suffix;

    /** image / video */
    private String category;

    /** MIME 类型 */
    private String contentType;

    private long fileSize;

    private long chunkSize;

    private int totalChunks;

    /** -1 表示不加入相册 */
    private Long albumId;

    /** 客户端文件最后修改时间（毫秒时间戳字符串） */
    private String lastModified;

    /** MinIO 原文件对象名 */
    private String fileObjectName;

    /** MinIO 缩略图对象名 */
    private String thumbnailObjectName;

    /** MinIO multipart uploadId */
    private String minioUploadId;

    private long createdAt;
}
