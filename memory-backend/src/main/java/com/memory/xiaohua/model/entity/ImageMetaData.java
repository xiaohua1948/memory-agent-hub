package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图像元数据实体类，包含图像的各类元数据信息
 */
@Data
@TableName("image_meta_data")
public class ImageMetaData {
    // 公共元数据：原始拍摄时间（来自EXIF信息，与图片表共享）
    @TableField(exist  = false)
    private LocalDateTime dateTimeOriginal;

    // 公共元数据：媒体宽度（像素级尺寸，与图片表共享）
    @TableField(exist = false)
    private Integer width;

    // 公共元数据：媒体高度（像素级尺寸，与图片表共享）
    @TableField(exist = false)
    private Integer height;

    // 公共元数据：设备制造商（如Apple/Sony，与图片表共享）
    @TableField(exist = false)
    private String make;

    // 公共元数据：设备型号（如iPhone 14 Pro，与图片表共享）
    @TableField(exist = false)
    private String model;

    // 公共元数据：纬度坐标（十进制格式，与图片表共享）
    @TableField(exist = false)
    private Double latitude;

    // 公共元数据：纬度参考方向（N-北纬/S-南纬，与图片表共享）
    @TableField(exist = false)
    private String latitudeRef;

    // 公共元数据：经度坐标（十进制格式，与图片表共享）
    @TableField(exist = false)
    private Double longitude;

    // 公共元数据：经度参考方向（E-东经/W-西经，与图片表共享）
    @TableField(exist = false)
    private String longitudeRef;

    @TableField
    private String fileId;
    // 处理图像的软件
    private String software;
    // 曝光程序
    private String exposureProgram;
    // 曝光时间（秒）
    private String exposureTime;
    // F数
    private Double fNumber;
    // ISO感光度
    private Integer iso;
    // 实际焦距（mm）
    private Double focalLength;
    // 等效35mm焦距
    private Double focalLength35;

    /**
     * GPS信息类
     */

    // 海拔高度
    private Double altitude;
    // 海拔高度参考基准
    private String altitudeRef;
    /**
     * EXIF信息内部类
     */

    // EXIF版本
    private String version;
    // 光圈值
    private Double apertureValue;
    // 快门速度值
    private Double shutterSpeed;
    // 测光模式
    private String meteringMode;
    // 白平衡模式
    private String whiteBalance;
    // 色彩空间
    private String colorSpace;
    // 传感器感应方式
    private String sensingMethod;
    // 拍摄对象距离（米）
    private Double subjectDistance;
    // 拍摄场景类型
    private String sceneType;

}