package com.memory.xiaohua.utils.metaData;


import com.memory.xiaohua.model.entity.VideoMetaData;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 视频元数据提取
 * @author: xiaohua
 * @date: 2025/2/19,21:14
 */
@Slf4j
@Component
public class VideoMetadataParserUtil {

    // #19 修复：删除调试 main 方法（含本地硬编码路径）

    private static void mapMetadata(VideoMetaData metadata, String key, String value) {
        switch (key.toLowerCase()) {

            // 时间信息
            case "creation_time":
                metadata.setDateTimeOriginal(parseQuickTimeDate(value));
                break;
            case "com.apple.quicktime.creationdate":  // iOS设备时间
                metadata.setDateTimeOriginal(parseQuickTimeDate(value));
                break;
            case "location":
                parseAndSetCoordinates(metadata, value);
            case "location-eng":// 格式：latitude,longitude
                parseAndSetCoordinates(metadata, value);
                break;
            // 设备信息
            case "com.android.manufacturer":
                metadata.setMake(value);
            case "manufacturer":
                metadata.setMake(value);
                break;
            case "com.android.model":
                metadata.setModel(value);
            case "model":
                metadata.setModel(value);
                break;

            case "color_space":
                metadata.setColorSpace(value);
                break;
            // 文件信息
            case "copyright":
                metadata.setCopyright(value);
                break;
            case "language":
                metadata.setLanguage(value);
                break;
            // 编码参数
            case "profile":
                metadata.setProfile(value);
                break;
            case "level":
                metadata.setLevel(value);
                break;
            case "pixel_format":
                metadata.setPixelFormat(value);
                break;
        }
    }

    public VideoMetaData GetVideoMetadata(String filePath) {
        VideoMetaData metadata = new VideoMetaData();
        //隐藏log
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePath)) {
            grabber.start();

            // 基础信息
            metadata.setDuration(grabber.getLengthInTime() / (double) avutil.AV_TIME_BASE);
            metadata.setWidth(grabber.getImageWidth());
            metadata.setHeight(grabber.getImageHeight());
            metadata.setFps(grabber.getVideoFrameRate());
            metadata.setVideoCodecName(grabber.getVideoCodecName());
            metadata.setVideoCodec(grabber.getVideoCodec());
            metadata.setVideoBitrate(grabber.getVideoBitrate());
            metadata.setRotation(grabber.getDisplayRotation());
            metadata.setAudioCodecName(grabber.getAudioCodecName());
            metadata.setAudioCodec(grabber.getAudioCodec());
            metadata.setAudioChannels(grabber.getAudioChannels());

            // 解析元数据字典
            Map<String, String> metadataMap = grabber.getMetadata();
            metadataMap.forEach((key, value) -> {
                mapMetadata(metadata, key, value);
            });
            grabber.stop();

        } catch (Exception e) {
            throw new RuntimeException("解析视频元数据失败: " + e.getMessage(), e);
        }
        return metadata;
    }

    /**
     * 获取视频元数据
     *
     * @param multipartFile 视频文件
     * @return 包含完整元数据的对象
     */
    public VideoMetaData GetVideoMetadata(MultipartFile multipartFile) {
        VideoMetaData metadata = new VideoMetaData();
        //隐藏log
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        InputStream inputStream = null;
        try {
            inputStream = multipartFile.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("视频流获取失败: " + e.getMessage(), e);
        }
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
            grabber.start();
            // 基础信息
            metadata.setDuration(grabber.getLengthInTime() / (double) avutil.AV_TIME_BASE);
            metadata.setWidth(grabber.getImageWidth());
            metadata.setHeight(grabber.getImageHeight());
            metadata.setRotation( grabber.getDisplayRotation());
            metadata.setFps(grabber.getVideoFrameRate());
            // 解析元数据字典
            Map<String, String> metadataMap = grabber.getMetadata();
            metadataMap.forEach((key, value) -> {

                mapMetadata(metadata, key, value);
            });
            grabber.stop();
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("解析视频元数据失败: " + e.getMessage(), e);
        }
        return metadata;
    }

    public byte[] extractCover(MultipartFile multipartFile) {
        try (InputStream inputStream = multipartFile.getInputStream();
             FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {

            // 1. 初始化视频解析
            grabber.start();

            // 2. 获取旋转元数据
             double rotate = grabber.getDisplayRotation();

            // 3. 获取首帧图像
            Frame frame = grabber.grabImage();
            if (frame == null) {
                throw new RuntimeException("无法获取视频首帧");
            }

            // 4. 转换并处理旋转
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.getBufferedImage(frame);

            image = applyRotation(image, (int) rotate);  // 关键修复点：应用旋转校正
            // 5. 输出处理后的图像
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("视频封面提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据旋转角度校正图像方向
     * @param source 原始图像
     * @param degrees 顺时针旋转角度（必须是90的倍数）
     * @return 校正后的图像
     */
    private BufferedImage applyRotation(BufferedImage source, int degrees) {
        if (degrees % 90 != 0) {
            throw new IllegalArgumentException("不支持的旋转角度: " + degrees);
        }

        // 计算实际旋转角度（转换为逆时针角度）
        int steps = degrees / 90;
        int realRotation = (4 - steps % 4) % 4;  // 转换为逆时针旋转次数

        // 无需旋转的情况
        if (realRotation == 0) return source;

        // 计算新画布尺寸（90/270度需要交换宽高）
        int width = source.getWidth();
        int height = source.getHeight();
        int newWidth = realRotation % 2 == 0 ? width : height;
        int newHeight = realRotation % 2 == 0 ? height : width;

        // 创建目标图像
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, source.getType());
        Graphics2D g2d = rotated.createGraphics();

        // 应用变换矩阵
        switch (realRotation) {
            case 1:  // 逆时针90度（等效顺时针270度）
                g2d.rotate(Math.toRadians(90));
                g2d.translate(0, -height);
                break;
            case 2:  // 180度
                g2d.rotate(Math.toRadians(180));
                g2d.translate(-width, -height);
                break;
            case 3:  // 逆时针270度（等效顺时针90度）
                g2d.rotate(Math.toRadians(270));
                g2d.translate(-width, 0);
                break;
        }

        // 绘制并释放资源
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        return rotated;
    }


    // 新增坐标解析方法
    private static void parseAndSetCoordinates(VideoMetaData metadata, String cordStr) {
        // 示例输入格式: "+34.1436+108.8665/" 或 "-12.3456-67.8901"
        String cleaned = cordStr.replaceAll("[/\\s]", "");

        Matcher matcher = Pattern.compile("^([+-]?\\d+\\.\\d+)([+-]?\\d+\\.\\d+)")
                .matcher(cleaned);

        if (matcher.find()) {
            // 解析纬度
            String latStr = matcher.group(1);
            metadata.setLatitude(Double.parseDouble(latStr));
            metadata.setLatitudeRef(latStr.startsWith("-") ? "S" : "N");

            // 解析经度
            String lonStr = matcher.group(2);
            metadata.setLongitude(Double.parseDouble(lonStr));
            metadata.setLongitudeRef(lonStr.startsWith("-") ? "W" : "E");
        } else {
            throw new IllegalArgumentException("无效的坐标格式: " + cordStr);
        }
    }
    // 辅助解析方法示例


    private static LocalDateTime parseQuickTimeDate(String value) {
        // 处理QuickTime特殊时间格式：2023-08-15T14:30:00+0800
        try {
            return LocalDateTime.parse(value,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX"));
        } catch (Exception e) {
            return null;
        }
    }


}
