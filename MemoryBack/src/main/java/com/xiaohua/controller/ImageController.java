package com.xiaohua.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaohua.domain.Image;

import com.xiaohua.domain.Operation;
import com.xiaohua.domain.VO.AllTimeTypeVO;
import com.xiaohua.domain.VO.ImageVO;
import com.xiaohua.domain.User;
import com.xiaohua.service.ImageService;
import com.xiaohua.service.RecordService;
import com.xiaohua.utils.FileServeUtil;
import com.xiaohua.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;



@RestController
@RequestMapping("/image")
public class ImageController {
    @Resource
    private FileServeUtil fileServeUtil;
    @Resource
    private ImageService imageService;
    @Resource
    private RecordService recordService;
    @Resource
    private TokenUtil tokenUtil;
    @Value("${file.upload.imgPath}")
    private String uploadPath;
    private  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private String defaultAlbum = "/static/album/albumImg.png";




    @RequestMapping("/upload")
    public JSONObject upload(HttpServletRequest req, String albumName, Integer albumId, String imgSite, String imgType,String imgDate, String token, @RequestParam("file")MultipartFile[] multipartFiles) throws Exception {
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        Integer userId;
        String userName;
       if(user!=null) {
           userId = user.getUserId();
           userName = user.getUserName();
       }
       else{
           jsonObject.put("status","fail");
           return jsonObject;
       }
        Date imageDate ;

        if(imgDate.equals("null")||imgDate.equals("")){
            System.out.println(1);
            imageDate=null;
        }
        else {
            imageDate = dateFormat.parse(imgDate);
        }
       if(imgSite==null||imgSite.equals("")){
           imgSite= "其它";
       }
        List<Image> imgList = new ArrayList<>();
        //将所有上传的图片对象存入集合
        for (MultipartFile m:multipartFiles){
            String fileName = m.getOriginalFilename();
            long fileSize = m.getSize();
            String imageUrL = fileServeUtil.uploadServe("img", userName, m);

            String compressUrL = fileServeUtil.CompressImage(fileServeUtil.ServPathToAP(imageUrL), userName, (float) m.getSize());

            imgList.add(new Image(null,fileName,fileSize,imgSite,imageUrL,compressUrL,imageDate));
        }
        System.out.println(imgList);
        boolean status = imageService.uploadImage(req,imgList, userId, albumId, albumName, imgType);

        if(status){
            jsonObject.put("status","success");
            //记录操作
            //

            recordService.addRecord(req, Operation.uploadImage.getName(),imgList.size(),userId);
        }
        else{
            for(Image i:imgList){
                String imageUrL = i.getImageUrL();
                fileServeUtil.deleteServe(imageUrL);
            }

            jsonObject.put("status","fail");
        }

        return jsonObject;
    }

    @RequestMapping("/uploadAI")
    public JSONObject uploadAI(HttpServletRequest req,String token, @RequestParam("file")MultipartFile[] multipartFiles,String albumName,Integer albumId,String imgSite, String imgDate,Integer resNumber) throws Exception {
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        Integer userId;
        String userName;
        if(user!=null) {
            userId = user.getUserId();
            userName = user.getUserName();
        }
        else{
            jsonObject.put("status","fail");
            return jsonObject;
        }
        Date imageDate ;
        if(imgDate.equals("null")||imgDate.equals("")){
            imageDate=null;
        }
        else {
            imageDate = dateFormat.parse(imgDate);
        }

        if(imgSite==null||imgSite.equals("")){
            imgSite= "其它";
        }
        List<Image> imgList = new ArrayList<>();
        //将所有上传的图片对象存入集合
        for (MultipartFile m:multipartFiles){
            String fileName = m.getOriginalFilename();
            long fileSize = m.getSize();
            String imageUrL = fileServeUtil.uploadServe("img", userName, m);
            String compressUrL = fileServeUtil.CompressImage(fileServeUtil.ServPathToAP(imageUrL), userName, (float) m.getSize());
            imgList.add(new Image(null,fileName,fileSize,imgSite,imageUrL,compressUrL,imageDate));
        }

        int count =1;
        if(resNumber!=null)
          count = resNumber ;
          JSONArray data = imageService.AIUploadImage(req,imgList,userId,albumId,albumName,count);
        if(data.size()>0){
            jsonObject.put("status","success");
            jsonObject.put("data",data);
            //记录操作
            //
            recordService.addRecord(req,Operation.AIUploadImage.getName(), imgList.size(),userId);
        }
        else{
            for(Image i:imgList){
                String imageUrL = i.getImageUrL();
                fileServeUtil.deleteServe(imageUrL);
            }
            jsonObject.put("status","fail");
        }

    return  jsonObject;
    }
    @RequestMapping("/selectTimeType")
    public JSONObject selectTimeType(String token){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        Integer userId;
        if(user!=null) {
            userId = user.getUserId();
        }
        else{
            jsonObject.put("status","fail");
            return jsonObject;
        }
        AllTimeTypeVO allTimeTypeVO = imageService.selectTimeType(userId);
        jsonObject.put("data",allTimeTypeVO);
        jsonObject.put("status","success");
        return jsonObject;
    }
    @RequestMapping("/selectAll")
    public JSONObject selectAllImage(String token,Integer currentPage,Integer pageSize){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        Integer userId;
        if(user!=null) {
            userId = user.getUserId();
        }
        else{
            jsonObject.put("status","fail");
            return jsonObject;
        }

        ImageVO  imageVO = imageService.selectAllImage(userId,currentPage,pageSize);
        jsonObject.put("data",imageVO);
        jsonObject.put("status","success");
        return jsonObject;
    }
    @RequestMapping("/selectAllByType")
    public JSONObject selectAllImageByType(String token,Integer currentPage,Integer pageSize,String imageType){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        System.out.println(user);
        Integer userId;
        if(user!=null) {
            userId = user.getUserId();
        }
        else{
            jsonObject.put("status","fail");
            return jsonObject;
        }
        ImageVO  imageVO = imageService.selectAllImageByType(userId,currentPage,pageSize,imageType);
        jsonObject.put("data",imageVO);
        jsonObject.put("status","success");
        return jsonObject;
    }


    @RequestMapping("/selectAllByTime")
    public JSONObject selectAllImageByTime(String token,Integer currentPage,Integer pageSize,String imageDate) throws ParseException {
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);

        Date imgDate = dateFormat.parse(imageDate);
        System.out.println(imageDate);
        Integer userId;
        if(user!=null) {
            userId = user.getUserId();
        }
        else{
            jsonObject.put("status","fail");
            return jsonObject;
        }
        ImageVO  imageVO = imageService.selectAllImageByTime(userId,currentPage,pageSize,imgDate);
        jsonObject.put("data",imageVO);
        jsonObject.put("status","success");
        return jsonObject;
    }


    @RequestMapping("/delete")
    public JSONObject deleteImage(HttpServletRequest req,String token, @RequestParam("imageId") List<Integer> imageId)  {
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        Integer userId;
        if(user!=null) {
            userId = user.getUserId();
        }
        else{
            jsonObject.put("status","fail");
            return jsonObject;
        }
        imageService.deleteImage(userId,imageId);
        jsonObject.put("status","success");
        recordService.addRecord(req,Operation.addRecycle.getName(), imageId.size(),userId);
        return jsonObject;
    }




}
