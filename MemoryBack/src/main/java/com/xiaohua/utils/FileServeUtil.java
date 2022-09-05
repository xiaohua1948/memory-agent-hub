package com.xiaohua.utils;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.imagerecog20190930.Client;
import com.aliyun.imagerecog20190930.models.TaggingImageAdvanceRequest;
import com.aliyun.imagerecog20190930.models.TaggingImageResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiaohua.domain.VO.AIResultVO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileServeUtil {
    @Value("${file.upload.imgPath}")
    private String uploadPath;
    @Value("${file.compressTemp.imgPath}")
    private String compressTempPath;
    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.accessKeySecret}")
    private String accessKeySecret;
    private String uploadPrefix = "/serve/";
    private String originPrefix = "/origin/";
    private String compressPrefix = "/compress/";
    float limitSize = 3145728;
    float minSize =   100000;
    public static void main(String[] args) {
        FileServeUtil fileServeUtil = new FileServeUtil();
        fileServeUtil.deleteServe("/serve/img/xiaofeng/1.jpg");
    }

    /**
     * 服务器存储路径转为绝对路径
     * @param ServePath
     * @return
     */
    public String ServPathToAP(String ServePath){
        String AbsolutePath = "";
        String path = ServePath.substring(uploadPrefix.length());
        AbsolutePath = uploadPath+path;
        return AbsolutePath;
    }


    public List<AIResultVO> imageGet(String ApPath)throws Exception {
        Config config = new Config();
        config.accessKeyId = accessKeyId;
        config.accessKeySecret = accessKeySecret;
        config.type="access_key";
        config.regionId="cn-shanghai";
        config.endpoint = "imagerecog.cn-shanghai.aliyuncs.com";
        Client client = new Client(config);
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        TaggingImageAdvanceRequest tar = new TaggingImageAdvanceRequest();
        InputStream inputStream = new FileInputStream(new File(ApPath));
        tar.imageURLObject=inputStream;
        TaggingImageResponse rsp = client.taggingImageAdvance(tar,runtimeOptions);
        //将json转为实体对象
        Gson gson = new Gson();
        List<AIResultVO> list = gson.fromJson( JSONObject.toJSONString(rsp.getBody().getData().tags), new TypeToken<ArrayList<AIResultVO>>(){}.getType());

        return list;
    }

    /**
     * 计算每个文件的UUId
     * @param file
     * @return
     */
    public String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
//            采用时间加MD5方式保证每个文件的唯一性
            String md5 = new String(Hex.encodeHex(MD5.digest()));
            Date date = new Date();
            long time = date.getTime();
            String str = md5+time;
            return str;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param uploadType   存储的图片类型
     * @param folderName   存储的文件夹名
     * @param multipartFile 文件流
     * @return 数据库中服务器文件的存储路径
     * @throws IOException
     */
    public String uploadServe(String uploadType,String folderName,MultipartFile multipartFile) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String MidPath="";
        if(uploadType.equals("img"))
         MidPath= uploadType+originPrefix+folderName+"/";
        else {
            MidPath = uploadType+"/"+folderName+"/";
        }
        File imgFile = new File(uploadPath+MidPath+originalFilename);
        if(!imgFile.getParentFile().exists()){
            imgFile.getParentFile().mkdirs();
        }
        multipartFile.transferTo(imgFile);
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String FileMd5 = getMD5(imgFile);
        String Path = MidPath+FileMd5+suffix;
        imgFile.renameTo(new File(uploadPath+Path));
        String ServePath = uploadPrefix+Path;
        return ServePath;
    }

    /**
     * 删除服务器文件
     * @param filePath
     */
    public void deleteServe(String filePath){
        String ApPath = ServPathToAP(filePath);
        new File(ApPath).delete();
    }

    public String CompressImage(String ApPath,String folderName ,Float imgSize) throws IOException {
        String MidPath= "img"+compressPrefix+folderName;
        File tempFile = new File(ApPath);
        if(!tempFile.getParentFile().exists()){
            tempFile.getParentFile().mkdirs();
        }
        //压缩比率
        float imgRate = limitSize>imgSize?(float) 0.9:(limitSize)/imgSize;

        String suffix = ApPath.substring(ApPath.lastIndexOf("/"));
        File imgFile = new File(uploadPath+MidPath+suffix);
        if(!imgFile.getParentFile().exists()){
            imgFile.getParentFile().mkdirs();
        }
//        最小100kb，否则不能压缩
        if(imgSize<minSize){
            copyFileUsingStream(new File(ApPath), new File(uploadPath + MidPath + suffix));
        }
        else{
            Thumbnails.of(ApPath).scale(1f).outputQuality(imgRate).toFile(uploadPath+MidPath+suffix);
        }

        return  uploadPrefix+MidPath+suffix;
    }
//    复制文件
    public void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }


}

