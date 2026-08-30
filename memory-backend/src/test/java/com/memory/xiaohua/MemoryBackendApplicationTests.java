package com.memory.xiaohua;

import com.memory.xiaohua.mapper.FaceMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.utils.picture.GeocoderLocationUtil;
import com.memory.xiaohua.utils.picture.OpenCVPictureUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


@SpringBootTest(classes = MemoryBackendApplication.class)
class MemoryBackendApplicationTests {


    @Resource
    OpenCVPictureUtil openCVPictureUtil;

    @Resource
    GeocoderLocationUtil geocoderLocationUtil;
    @Resource
    MinioUtil minioUtil;

    @Resource
    FaceMapper faceMapper;

    @Resource
    FileMapper fileMapper;

    @Test
    void isSamePicture() throws IOException{
        byte[] byte1;
        byte[] byte2;
        File file = new File("F:/Desktop/4.jpg");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte1 = new byte[(int) file.length()];
            fis.read(byte1);  // 读取全部字节到数
        }
        File file2 = new File("F:/Desktop/5.jpg");
        try (FileInputStream fis = new FileInputStream(file2)) {
            byte2 = new byte[(int) file2.length()];
            fis.read(byte2);  // 读取全部字节到数
        }
         boolean v = openCVPictureUtil.isSamePicture(0.8,byte1, byte2);
        System.out.println(v);
    }
    @Test
    void location(){
        GeocoderLocationUtil.Regeocode regeocode = geocoderLocationUtil.reverseGeocode(107.77191388888889, 32.98692777777778);
        System.out.println(regeocode);
    }

//    @Test
//    void contextLoads() throws IOException {
//        byte[] bytes;
//        byte[] byte2;
//        File file = new File("F://pictures//卡通//pictures//人物//刘亦菲//1 (9).jpg");
//        try (FileInputStream fis = new FileInputStream(file)) {
//             bytes = new byte[(int) file.length()];
//            fis.read(bytes);  // 读取全部字节到数
//        }
//        File file2 = new File("F://pictures//卡通//pictures//人物//刘亦菲//1 (10).jpg");
//        try (FileInputStream fis = new FileInputStream(file2)) {
//            byte2 = new byte[(int) file2.length()];
//            fis.read(byte2);  // 读取全部字节到数
//        }
//        byte[] bytes1 = personFaceUtil.extractFace(bytes);
//        byte[] bytes2 = personFaceUtil.extractFace(byte2);
//        boolean samePerson = personFaceUtil.isSamePerson(bytes1, bytes2);
//        System.out.println(samePerson);
//    }
//
//    @Test
//    void updateFileUrl(){
//        Wrapper<FileEntity> queryWrapper = new QueryWrapper<>();
//        UpdateWrapper<FileEntity> updateWrapper = new UpdateWrapper<>();
//        List<FileEntity> fileEntities = fileMapper.selectList(queryWrapper);
//        for (FileEntity fileEntity : fileEntities) {
//            String fileId = fileEntity.getFileId();
//            String fileObjectName = fileEntity.getFileObjectName();
//            String thumbnailObjectName = fileEntity.getThumbnailObjectName();
//            String fileUrl = minioUtil.getFileUrl(fileObjectName);
//            String thumbnailUrl = minioUtil.getFileUrl(thumbnailObjectName);
//            updateWrapper.eq("file_id",fileId);
//            updateWrapper.set("file_url",fileUrl);
//            updateWrapper.set("thumbnail_url",thumbnailUrl);
//        }
//
//    }

}
