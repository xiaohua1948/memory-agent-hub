package com.memory.xiaohua.utils.picture;

import jakarta.annotation.PostConstruct;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/7,13:20
 */
@Component
public class OpenCVPictureUtil {
    @Value("${opencv.native.dll-path}")
    private String dllPath;

    @Value("${opencv.native.haar-model}")
    private String xmlPath;
    //人物相似度阈值
    private static final double Person_SIMILARITY = 0.72;
    //图片相似度阈值
    private static final double Picture_SIMILARITY = 0.75;
    private  CascadeClassifier FACE_DETECTOR;
    private static final double SCALE_FACTOR = 1.1;
    private static final int MIN_NEIGHBORS = 5;
    private static final int STANDARD_SIZE = 150;
    //最小尺寸
    private static final Size MIN_SIZE = new Size(30, 30);


    @PostConstruct
    public void init() {
        try {
        ClassPathResource dllResource = new ClassPathResource(dllPath);
        String absolutePath = dllResource.getFile().getAbsolutePath();
        System.load(absolutePath);
        // 1. 加载OpenCV本地库
        // 2. 加载分类器（带路径解码）
        Resource resource = new ClassPathResource(xmlPath);

            FACE_DETECTOR = new CascadeClassifier(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (FACE_DETECTOR.empty()) {
            throw new RuntimeException("分类器初始化失败，请检查OpenCV版本兼容性");
        }
    }


    /**
     * @return 是否成功
     */
    public  byte[] extractFace(byte[] srcBytes) {
        try {
            // 1. 字节数组转Mat对象（参考网页11）
            MatOfByte matOfByte = new MatOfByte(srcBytes);
            Mat image = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

            if (image.empty()) {
                System.err.println("[ERROR] 无法解码图像数据");
                return null;
            }


            // 2. 多尺度人脸检测（参考网页1、8）
            MatOfRect faceDetections = new MatOfRect();
            FACE_DETECTOR.detectMultiScale(image, faceDetections, SCALE_FACTOR,
                    MIN_NEIGHBORS, 0, MIN_SIZE, new Size());
            // 3. 检测结果校验（参考网页7）
            if (faceDetections.empty()) {
                System.out.println("[INFO] 未检测到人脸");
                return null;
            }
            // 4. 提取最大人脸（参考网页3）
            Rect maxFace = faceDetections.toList().stream()
                    .max(Comparator.comparingInt(r -> r.width * r.height))
                    .orElse(null);

            // 5. 图像处理（参考网页3、11）
            Mat cropped = null;
            if (maxFace != null) {
                cropped = new Mat(image, maxFace);
            }
            Mat resized = new Mat();
            if (cropped != null) {
                Imgproc.resize(cropped, resized, new Size(STANDARD_SIZE,STANDARD_SIZE));
            }
            return matToBytes(resized);
        } catch (Exception e) {
            System.err.println("[EXCEPTION] 处理失败: " + e.getMessage());
        }
        return null;
    }

    // Mat转字节数组（修正索引）
    public  byte[] matToBytes(Mat featureVector) {
        // 验证矩阵维度
        // 验证输入
        if (featureVector.rows() != STANDARD_SIZE || featureVector.cols() != STANDARD_SIZE
                || featureVector.channels() != 3) {
            throw new IllegalArgumentException("需 "+STANDARD_SIZE+"px RGB图像");
        }

        // 使用PNG格式保持无损压缩
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", featureVector, mob);
        return mob.toArray();
    }

    public  Mat bytesToMat(byte[] bytes) {
        return Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR
        );
    }


    /**
     * 人像比对（双重验证机制）
     */
    public  boolean isSamePerson(byte[] byte1, byte[] byte2) {
        Mat face1 = bytesToMat(byte1);
        Mat face2 = bytesToMat(byte2);

        // 直方图相似度验证[5](@ref)
        double histScore = compareHistogram( (face1),  (face2));

        return histScore >= Person_SIMILARITY;
    }

    /**
     * 直方图对比
     */
    private  double compareHistogram(Mat img1, Mat img2) {
        Mat hist1 = new Mat(), hist2 = new Mat();
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);

        Imgproc.calcHist(List.of(toGray(img1)), new MatOfInt(0), new Mat(),
                hist1, histSize, ranges);
        Imgproc.calcHist(List.of(toGray(img2)), new MatOfInt(0), new Mat(),
                hist2, histSize, ranges);

        Core.normalize(hist1, hist1);
        Core.normalize(hist2, hist2);
        return 1 - Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_BHATTACHARYYA);
    }

    /**
     * 灰度处理
     */
    private  Mat toGray(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }


    public boolean isSamePicture(Double similarity,byte[] byte1, byte[] byte2) {
        // 将字节数组解码为Mat对象
        Mat img1 = bytesToMat(byte1);
        Mat img2 = bytesToMat(byte2);

        // 检查图像是否有效
        if (img1.empty() || img2.empty()) {
            throw new IllegalArgumentException("输入图片数据无效，无法解码为图像");
        }

        // 转换为HSV颜色空间
        Mat hsv1 = new Mat();
        Mat hsv2 = new Mat();
        Imgproc.cvtColor(img1, hsv1, Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(img2, hsv2, Imgproc.COLOR_BGR2HSV);

        // 设置直方图参数
        int hBins = 50;  // 色调通道的bin数量
        int sBins = 60;  // 饱和度通道的bin数量
        MatOfInt histSize = new MatOfInt(hBins, sBins);
        MatOfFloat ranges = new MatOfFloat(0f, 180f, 0f, 256f);  // H范围0-179，S范围0-255
        MatOfInt channels = new MatOfInt(0, 1);  // 使用H和S通道

        // 计算直方图
        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        Imgproc.calcHist(List.of(hsv1), channels, new Mat(), hist1, histSize, ranges);
        Imgproc.calcHist(List.of(hsv2), channels, new Mat(), hist2, histSize, ranges);

        // 归一化直方图
        Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX);
        Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX);

        // 使用巴氏距离比较直方图，并转换为相似度得分
        double distance = Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_BHATTACHARYYA);

        return (1 - distance)>= similarity ;
    }
}