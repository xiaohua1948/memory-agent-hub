package com.memory.xiaohua.model.dto;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/20,22:51
 */
import lombok.Builder;
import lombok.Data;


@Data
public class FileChunkDTO {

    /**
     * 当前分片序号（从1开始）
     */
    private Integer chunkNumber;

    /**
     * 分片大小（字节）
     */
    private Long chunkSize;

    /**
     * 当前分片实际大小（字节）
     */
    private Long currentChunkSize;

    /**
     * 文件总大小（字节）
     */
    private Long totalSize;

    /**
     * 文件唯一标识（MD5）
     */

    private String identifier;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 文件相对路径
     */
    private String relativePath;

    /**
     * 总分片数
     */

    private Integer totalChunks;

    /**
     * 最后修改时间戳
     */
    private Long lastModified;

    /**
     * 上传相册的id
     */
    private Long albumId;
}