package com.memory.xiaohua.model.vo.entity;

import lombok.Data;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/21,0:21
 */
@Data
public class FileInfo {
    /**
     * 文件唯一ID UUID
     */
    private String fileId;
    /**
     * 外键用户id
     */
    private Long userId;

    /**
     * 原始文件名（带扩展名）
     */
    private String originFileName;

    /**
     * 文件大小（单位：字节）
     */
    private Long size;

    /**
     * MIME类型（例：image/png）
     */
    private String contentType;

    /**
     * 文件分类（自定义类型：image/doc/video等）
     */
    private String category;

    /**
    * 视频时长(图片为0)
    */
    private Integer duration;

    private Integer width;

    private Integer height;

    /**
     * 文件访问URL（CDN地址）
     */
    private String fileUrl;

    /**
     * 缩略图URL（图片专用）
     */
    private String thumbnailUrl;
    /**
     * 缩略图object（图片专用）
     */
    private String thumbnailObjectName;
}
