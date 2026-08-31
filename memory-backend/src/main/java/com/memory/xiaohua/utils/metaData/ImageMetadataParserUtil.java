package com.memory.xiaohua.utils.metaData;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.memory.xiaohua.model.entity.ImageMetaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * @description: 图片元数据提取
 * @author: xiaohua
 * @date: 2025/2/19,21:14
 */
@Slf4j
@Component
public class ImageMetadataParserUtil {

    // #19 修复：删除调试 main 方法（含本地硬编码路径）
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    /**
     * @Description: 获取图片媒体信息
     * @Author: xiaohua
     * @Date: 2025/2/5,20:04:42
     */
    public ImageMetaData getImageMetadata(MultipartFile file) throws IOException, ImageProcessingException {
        return getImageMetadata(file.getInputStream());
    }

    /**
     * 从输入流解析图片元数据。
     * 分片合并后文件已存于对象存储，可直接流式解析，无需回落到 MultipartFile。
     */
    public ImageMetaData getImageMetadata(InputStream inputStream) throws IOException, ImageProcessingException {
        Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
        ImageMetaData imageMetadata = new ImageMetaData();
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName();
                String description = tag.getDescription();

                try {
                    mapTagToField(imageMetadata, tagName, description);
                } catch (Exception e) {
                    log.warn("元数据解析异常 tag={}", tagName, e);
                }
            }
        }
        return imageMetadata;
    }
    public static ImageMetaData getImageMetadata(File file) throws IOException, ImageProcessingException {
        Metadata metadata;
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = fileInputStream.readAllBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

        metadata = ImageMetadataReader.readMetadata(byteArrayInputStream);
        ImageMetaData imageMetadata = new ImageMetaData();
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName();
                String description = tag.getDescription();


                    mapTagToField(imageMetadata, tagName, description);


            }
        }
        return imageMetadata;
    }


    /**
     * @Description: 将图片信息实例化
     * @Author: xiaohua
     * @Date: 2025/2/5,19:59:48
     */
    private static void mapTagToField(ImageMetaData imageMetadata, String tagName, String description) {
        switch (tagName) {
            case "Image Width":
                imageMetadata.setWidth(Integer.parseInt(description.replace(" pixels", "")));
                break;
            case "Image Height":
                imageMetadata.setHeight(Integer.parseInt(description.replace(" pixels", "")));
                break;
            case "Make":
                imageMetadata.setMake(description);
                break;
            case "Model":
                imageMetadata.setModel(description);
                break;
            case "Software":
                imageMetadata.setSoftware(description);
                break;
            case "Date/Time Original":
                imageMetadata.setDateTimeOriginal(LocalDateTime.parse(description, DATE_TIME_FORMATTER));
                break;
            case "Exposure Program":
                imageMetadata.setExposureProgram(description);
                break;
            case "Exposure Time":
                imageMetadata.setExposureTime(parseExposureTime(description));
                break;
            case "F-Number":
                imageMetadata.setFNumber(Double.parseDouble(description.replace("F", "")));
                break;
            case "ISO Speed Ratings":
                imageMetadata.setIso(Integer.parseInt(description));
                break;
            case "Focal Length":
                imageMetadata.setFocalLength(Double.parseDouble(description.replace(" mm", "")));
                break;
            case "Focal Length 35":
                imageMetadata.setFocalLength35(Double.parseDouble(description.replace("mm", "")));
                break;
            case "GPS Latitude":
                imageMetadata.setLatitude(parseGpsCoordinate(description));
                break;
            case "GPS Latitude Ref":
                imageMetadata.setLatitudeRef(description);
                break;
            case "GPS Longitude":
                imageMetadata.setLongitude(parseGpsCoordinate(description));
                break;
            case "GPS Longitude Ref":
                imageMetadata.setLongitudeRef(description);
                break;
            case "GPS Altitude":
                imageMetadata.setAltitude(Double.parseDouble(description.replace(" metres", "")));
                break;
            case "GPS Altitude Ref":
                imageMetadata.setAltitudeRef(description);
                break;
            case "Exif Version":
                imageMetadata.setVersion(description);
                break;
            case "Aperture Value":
                imageMetadata.setApertureValue(Double.parseDouble(description.replace("F", "")));
                break;
            case "Shutter Speed Value":
                imageMetadata.setShutterSpeed(parseShutterSpeed(description));
                break;
            case "Metering Mode":
                imageMetadata.setMeteringMode(description);
                break;
            case "White Balance":
                imageMetadata.setWhiteBalance(description);
                break;
            case "Color Space":
                imageMetadata.setColorSpace(description);
                break;
            case "Sensing Method":
                imageMetadata.setSensingMethod(description);
                break;
            case "Subject Distance":
                imageMetadata.setSubjectDistance(Double.parseDouble(description.replace(" metres", "")));
                break;
            case "Scene Type":
                imageMetadata.setSceneType(description);
                break;
        }
    }

    private static String parseExposureTime(String value) {
        return value.replace(" sec", "");
    }

    private static double parseShutterSpeed(String value) {
        String[] parts = value.split("/");
        if (parts.length == 2) {
            try {
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static double parseGpsCoordinate(String value) {
        String[] parts = value.split("[°'\"]");
        if (parts.length >= 3) {
            try {
                double degrees = Double.parseDouble(parts[0].trim());
                double minutes = Double.parseDouble(parts[1].trim());
                double seconds = Double.parseDouble(parts[2].trim());
                return degrees + (minutes / 60) + (seconds / 3600);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }


}