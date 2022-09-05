package com.xiaohua.controller;


import com.alibaba.fastjson.JSONObject;
import com.xiaohua.dao.RecordMapper;
import com.xiaohua.domain.Image;
import com.xiaohua.domain.Operation;
import com.xiaohua.domain.User;
import com.xiaohua.domain.VO.ImageVO;
import com.xiaohua.service.RecordService;
import com.xiaohua.service.RecycleService;
import com.xiaohua.utils.TokenUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/recycle")
public class RecycleController {
    @Resource
    RecycleService recycleService;
    @Resource
    RecordService recordService;
    @Resource
    TokenUtil tokenUtil;

    @RequestMapping("/selectAll")
    public JSONObject selectImage(String token){
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
        JSONObject res = recycleService.selectAll(userId);
        System.out.println(res);
        jsonObject.put("data",res);
        jsonObject.put("status","success");
        return jsonObject;
    }
    @RequestMapping("/recoverImage")
    public JSONObject recoverImage(HttpServletRequest req,String token,@RequestParam("imageId")List<Integer> imageId){
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
        System.out.println(imageId);
        recycleService.recoverImage(userId,imageId);
        jsonObject.put("status","success");
        recordService.addRecord(req,Operation.recoverImage.getName(), imageId.size(),userId);
        return jsonObject;
    }
    @RequestMapping("/deleteImage")
    public JSONObject deleteImage(HttpServletRequest req, String token, @RequestParam("imageId")List<Integer> imageId){
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
        recycleService.deleteImage(userId,imageId);
        jsonObject.put("status","success");

        recordService.addRecord(req, Operation.deleteImage.getName(), imageId.size(),userId);
        return jsonObject;
    }


}
