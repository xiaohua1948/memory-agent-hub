package com.memory.xiaohua.model.dto.upload;

import lombok.Data;

/**
 * 合并完成响应
 */
@Data
public class UploadCompleteVO {

    /** 文件 ID */
    private String fileId;

    /** 原文件名 */
    private String originFileName;

    /** image / video */
    private String category;

    /** 原图访问 URL（预签名，有时效） */
    private String fileUrl;

    /** 缩略图访问 URL（预签名，有时效） */
    private String thumbnailUrl;

    private Integer width;

    private Integer height;

    private Long size;

    /** 是否为秒传（兼容信息展示） */
    private boolean instant;
}
