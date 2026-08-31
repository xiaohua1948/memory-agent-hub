package com.memory.xiaohua.model.dto.upload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 合并分片请求
 */
@Data
public class UploadCompleteRequest {

    @NotBlank(message = "uploadId 不能为空")
    private String uploadId;

    /** 整个文件的 MD5，用于服务端二次校验 */
    @NotBlank(message = "文件 MD5 不能为空")
    private String md5;
}
