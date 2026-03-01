package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @description: 文件实体类
 * @author: xiaohua
 * @date: 2025/2/19,20:42
 */
@Data
@TableName("file")
public class FileEntity {
    @TableId(type = IdType.INPUT)
    /** 文件唯一ID UUID */
    private String fileId;

    /** 原始文件名（带扩展名） */
    private String originFileName;

    /** 文件大小（单位：字节） */
    private Long size;

    /** 客户端最后修改时间 */
    private LocalDateTime lastModifiedTime;
    /** 上传云端时间 */
    @TableField(exist = false)
    private LocalDateTime uploadTime;

    /** MIME类型（例：image/png） */
    private String contentType;

    /** 文件分类（image/doc/video等） */
    private String category;

    /** 文件访问URL（CDN地址） */
    private String fileUrl;

    /** 缩略图URL（图片专用） */
    private String thumbnailUrl;

    /** 对象存储路径 */
    private String fileObjectName;

    private String thumbnailObjectName;

    /** 文件hash */
    private String md5;

    /** 文件地理位置（格式化字符串） */
    private String location;

    // 媒体元数据字段
    private LocalDateTime dateTimeOriginal; // 原始拍摄时间
    private Integer width;                  // 媒体宽度（像素）
    private Integer height;                 // 媒体高度（像素）
    private String make;                    // 设备制造商
    private String model;                   // 设备型号

    // GPS地理坐标
    private Double latitude;               // 纬度
    private String latitudeRef;            // 纬度参考方向
    private Double longitude;               // 经度
    private String longitudeRef;            // 经度参考方向
}