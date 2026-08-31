package com.memory.xiaohua.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分片上传配置
 *
 * <p>注意：{@code spring.servlet.multipart.max-file-size} 必须 >= chunkSize，
 * 因为每个请求只携带一个分片，而非整个文件。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadConfig {

    /**
     * 分片大小（字节），默认 5MB。
     * S3/MinIO multipart 要求除最后一片外每片不小于 5MB，且总分片数不超过 10000。
     */
    private long chunkSize = 5L * 1024 * 1024;

    /** 单文件最大大小（字节），默认 2GB */
    private long maxFileSize = 2L * 1024 * 1024 * 1024L;

    /**
     * 服务端 MD5 全量校验的文件大小上限（字节），默认 200MB。
     * 超过该大小则跳过校验，避免大文件长时间阻塞合并接口。
     */
    private long md5VerifyMaxSize = 200L * 1024 * 1024;

    /** 是否开启服务端 MD5 校验 */
    private boolean md5VerifyEnabled = true;

    /** 上传任务有效期（小时），超时后任务与已上传分片会被清理 */
    private int taskTtlHours = 24;

    /** 单用户并发上传任务数上限 */
    private int maxConcurrentTasks = 5;

    /** 图片超过该大小（字节）才压缩生成缩略图，否则直接复用原图 */
    private long thumbnailThreshold = 500L * 1000;

    /** 缩略图长边像素 */
    private int thumbnailMaxSize = 512;

    /** 缩略图输出质量 0~1 */
    private double thumbnailQuality = 0.75;

    /**
     * 计算总分片数
     */
    public int calcTotalChunks(long fileSize) {
        if (fileSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) fileSize / (double) chunkSize);
    }

    /**
     * 除最后一片外，每片必须等于 chunkSize
     */
    public boolean isChunkSizeValid(int chunkIndex, int totalChunks, long actualSize) {
        if (chunkIndex < totalChunks - 1) {
            return actualSize == chunkSize;
        }
        return actualSize > 0;
    }
}
