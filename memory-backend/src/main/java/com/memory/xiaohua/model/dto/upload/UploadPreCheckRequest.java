package com.memory.xiaohua.model.dto.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 上传预检请求
 *
 * <p>用于秒传判定与断点续传。携带 uploadId 时表示"续传已存在的任务"。</p>
 */
@Data
public class UploadPreCheckRequest {

    /** 原始文件名（带扩展名） */
    @NotBlank(message = "文件名不能为空")
    private String filename;

    /** 文件总大小（字节） */
    @NotNull(message = "文件大小不能为空")
    private Long size;

    /** 整个文件的 MD5，秒传与去重依据 */
    @NotBlank(message = "文件 MD5 不能为空")
    private String md5;

    /** 客户端文件最后修改时间（毫秒时间戳） */
    private String lastModified;

    /** 目标相册 ID，-1 表示不加入相册 */
    private Long albumId = -1L;

    /**
     * 可选的上传任务 ID。
     * 前端本地若保存过未完成的 uploadId，可带上以续传（服务端返回已上传分片清单）。
     */
    private String uploadId;
}
