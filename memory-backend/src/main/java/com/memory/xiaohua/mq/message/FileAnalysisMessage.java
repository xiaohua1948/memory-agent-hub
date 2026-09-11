package com.memory.xiaohua.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件异步分析事件。
 *
 * <p>上传主流程落库后投递，携带各分析任务所需的全部上下文；
 * 由逆地理编码 / 图像分类识别 / 人脸检测三类独立消费者组按 tag 分别消费，
 * 上传接口无需等待这些耗时操作（第三方 API、OpenCV 计算）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAnalysisMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件主表 id */
    private String fileId;

    /** 文件归属用户 id */
    private Long userId;

    /** 文件分类（image/video） */
    private String category;

    /** OSS 原图对象名 */
    private String objectName;

    /** OSS 缩略图对象名（图像分类识别 / 人脸检测使用） */
    private String thumbnailObjectName;

    /** 经度（逆地理编码使用） */
    private Double longitude;

    /** 纬度（逆地理编码使用） */
    private Double latitude;
}
