package com.xiaohua.controller;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.domain.Operation;
import com.xiaohua.domain.Record;
import com.xiaohua.domain.User;
import com.xiaohua.service.RecordService;
import com.xiaohua.utils.TokenUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/record")
public class RecordController {
    @Resource
    RecordService recordService;
    @Resource
    TokenUtil tokenUtil;

    @RequestMapping("/recordDownload")
    public JSONObject recordDownload(HttpServletRequest req, String token,Integer number){
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
        recordService.addRecord(req, Operation.downloadImage.getName(),number,userId);
        jsonObject.put("status","success");
        return jsonObject;
    }

    @RequestMapping("/selectAll")
    public JSONObject selectAllRecord(String token) throws ParseException {
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
        List<Record> records = recordService.selectAllRecord(userId);
        jsonObject.put("status","success");
        jsonObject.put("data",records);
        return jsonObject;
    }


    @RequestMapping("/deleteAll")
    public JSONObject deleteAllRecord(String token){
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
        recordService.deleteAllRecord(userId);
        jsonObject.put("status","success");
        return jsonObject;
    }


    @RequestMapping("/deleteById")
    public JSONObject deleteRecordById(String token,@RequestParam("Ids")List<Integer> Ids){
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
        recordService.deleteRecordByIds(userId,Ids);
        jsonObject.put("status","success");
        return jsonObject;
    }
}
