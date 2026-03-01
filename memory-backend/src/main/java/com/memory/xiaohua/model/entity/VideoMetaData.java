package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 */
@Data
@TableName("video_meta_data")
public class VideoMetaData {
    @TableField
    // 公共基础信息
    private String fileId;
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



    private String colorSpace;       // 色彩空间
    // 视频特有属性
    private Double duration;          // 时长（秒）
    private Double fps;               // 帧率
    // 视频编码属性
    private String videoCodecName;// 视频编码格式
    private Integer videoCodec;
    private Integer videoBitrate;    // 视频码率
    private Double rotation;        // 旋转角度
    // 音频编码属性
    private String audioCodecName;   // 音频编码格式
    private  Integer AudioCodec;
    private Integer audioSampleRate; // 采样率
    private Integer audioChannels;   // 声道数
    // 高级属性
    private String copyright;        // 版权信息
    private String language;         // 语言

    // 编码参数
    private String profile;          // 编码配置
    private String level;            // 编码级别
    private String pixelFormat;      // 像素格式

}