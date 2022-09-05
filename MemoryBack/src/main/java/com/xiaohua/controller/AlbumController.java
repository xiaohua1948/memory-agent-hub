package com.xiaohua.controller;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.domain.VO.PartAlbumVO;
import com.xiaohua.domain.User;
import com.xiaohua.service.AlbumService;
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
@RequestMapping("/album")
public class AlbumController {
    @Resource
    AlbumService albumService;
    @Resource
    RecordService recordService;
    @Resource
    TokenUtil tokenUtil;

    @RequestMapping("/selectAlbumName")
    public JSONObject selectAlbumName(String token){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        List<PartAlbumVO> partAlbumVOS = albumService.selectAllAlbum(user.getUserId());
        jsonObject.put("status","success");
        jsonObject.put("data", partAlbumVOS);
        return jsonObject;
    }
    @RequestMapping("/addImageToAlbum")
    public JSONObject addImageToAlbum(HttpServletRequest req, String token, @RequestParam("imageId") List<Integer> imageId, Integer albumId ){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        albumService.addImageToAlbum(req,albumId,imageId,user.getUserId());
        jsonObject.put("status","success");
        return jsonObject;
    }

    @RequestMapping("/removeImageFromAlbum")
    public JSONObject removeImageFromAlbum(HttpServletRequest req, String token, @RequestParam("imageId") List<Integer> imageId, Integer albumId ){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        albumService.removeImageToAlbum(req,albumId,imageId,user.getUserId());
        jsonObject.put("status","success");
        return jsonObject;
    }

    @RequestMapping("/addAlbum")
    public JSONObject addAlbum(HttpServletRequest req,String token,String albumName){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        albumService.addAlbum(req,albumName,user.getUserId());
        jsonObject.put("status","success");
        return jsonObject;
    }

    @RequestMapping("/deleteAlbumByIds")
    public JSONObject deleteAlbumByIds(HttpServletRequest req,String token,@RequestParam("albumIds")List<Integer> albumIds){
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        albumService.deleteAlbum(req,albumIds,user.getUserId());
        jsonObject.put("status","success");

        return jsonObject;
    }


    @RequestMapping("/selectAllImage")
    public JSONObject selectAllImage(HttpServletRequest req,String token,Integer albumId) throws ParseException {
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        JSONObject res = albumService.selectAlbumImage(albumId);

        jsonObject.put("status","success");
        jsonObject.put("data",res);

        return jsonObject;
    }
    @RequestMapping("/setAlbumCover")
    public JSONObject setAlbumCover(String token,Integer albumId,Integer imageId) throws ParseException {
        JSONObject jsonObject = new JSONObject();
        User user = tokenUtil.jwtParser(token);
        if(user==null){
            jsonObject.put("status","fail");
            return jsonObject;
        }
        albumService.setAlbumCover(albumId,imageId);
        jsonObject.put("status","success");
        return jsonObject;
    }





}
