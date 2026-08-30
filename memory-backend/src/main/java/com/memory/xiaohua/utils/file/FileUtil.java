package com.memory.xiaohua.utils.file;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.MessageDigest;
import java.util.Set;


/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/10,19:43
 */
@Component
public class FileUtil {

    // 支持的图片类型集合（统一小写带点）
    private static final Set<String> ACCEPT_IMAGE_TYPES = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp"
    );

    // 支持的视频类型集合（统一小写带点）
    private static final Set<String> ACCEPT_VIDEO_TYPES = Set.of(
            ".mp4", ".rmvb", ".mkv", ".wmv", ".flv"
    );

    /**
     * 判断文件类型
     * @param suffix 文件后缀（如 ".mp4" 或 "jpg"）
     * @return "image"（图片）、"video"（视频）或空字符串（未知类型）
     */
    public  String getFileType(String suffix) {
        suffix = suffix.toLowerCase();
        if (ACCEPT_IMAGE_TYPES.contains("."+suffix)) {
            return "image";
        } else if (ACCEPT_VIDEO_TYPES.contains("."+suffix)) {
            return "video";
        } else {
            return "";
        }
    }

    /**
     * 创建分辨率为640x480的略缩图，并在此基础上，保留原始图像的纵横比
     */
    public static void formatResolution(File source,File target)  {
        try {
            Thumbnails
                    //传入源文件
                    .of(source)
                    //生成缩略图的宽高
                    .size(300, 300)
                    //保留原始图像纵横比
                    .keepAspectRatio(true)
                    //目标文件
                    .toFile(target);
        } catch (IOException e) {
            throw new RuntimeException("压缩图片元失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算MD5
     */
    public String getMD5(InputStream inputStream)  {
        // 2.8 修复：用 try-with-resources 确保异常时流也关闭
        try (inputStream) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(md5.digest()));
        }
        catch (Exception e) {
            throw new RuntimeException("计算Md5失败: " + e.getMessage(), e);
        }
    }
    /**
     *  保持原始分辨率，减低图片质量
     */
    public  ByteArrayOutputStream outputQuality(byte[] source,String fileName)  {
        ByteArrayOutputStream thumbOutput = new ByteArrayOutputStream();
        try {
            Thumbnails
                    //传入源文件
                    .of(new ByteArrayInputStream(source))
                    //原始分辨率
                    .scale(0.15)
                    //输出图片质量，该值介于 double 和 1.0d 和 0.0d之间，值越高质量越高
                    .outputQuality(0.65)

                    //目标文件
                    .toOutputStream(thumbOutput);
        } catch (IOException e) {
            throw new RuntimeException("压缩图片元失败: " + e.getMessage(), e);
        }
        return thumbOutput;
    }


}
