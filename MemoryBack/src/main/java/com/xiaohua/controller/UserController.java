package com.xiaohua.controller;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.domain.User;
import com.xiaohua.service.UserService;
import com.xiaohua.utils.FileServeUtil;
import com.xiaohua.utils.RedisUtil;
import com.xiaohua.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private TokenUtil tokenUtil;
    @Resource
    RedisUtil redisUtil;
    @Resource
    private UserService userService;  //用户服务
    @Resource
    private FileServeUtil fileServeUtil;
    @Value("${file.upload.imgPath}")
    private String uploadPath;
    //默认头像
    private String defaultAvatar = "/static/avatar/1default.jpg";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


    /**
     * 用户注册
     * @param userName
     * @param passWord
     * @param email
     * @param codeNumber
     * @return
     */
    @RequestMapping("/add")
    public JSONObject addUser(@RequestParam("username")String userName,@RequestParam("password")String passWord,String email,String codeNumber){
        JSONObject jsonObject = new JSONObject();
        if(userService.selectUserName(userName)!=null){
            jsonObject.put("status","exist");
            return jsonObject;
        }


        System.out.println(email);
        String redisCode = redisUtil.get(email);
        if(!codeNumber.equals(redisCode)){
            jsonObject.put("status","codeError");
            return jsonObject;
        }
        User user = new User(null,userName,passWord,null,email,null,null,null,5000,defaultAvatar);
        userService.addUser(user);
        jsonObject.put("status","success");
        return jsonObject;
    }

    /**
     * 用户登录
     * @param userName
     * @param passWord
     * @return
     */
    @RequestMapping("/login")
    public JSONObject selectUser(String userName,String passWord){
        User user = new User(userName,passWord);
        Integer userId = userService.selectUserId(user);
        JSONObject jsonObject = new JSONObject();
        if(userId==null){
            jsonObject.put("status","fail");
            jsonObject.put("token","-1");
            return jsonObject;
        }
        else {
            String token = tokenUtil.createToken(user.getUserName(), userId);
            jsonObject.put("status", "success");
            jsonObject.put("token", token);
            System.out.println(jsonObject);
            System.out.println(token);
            return jsonObject;
        }
    }

    /**
     * 用户查询
     * @param token
     * @return
     */
    @RequestMapping("/selectUser")
    public JSONObject selectUser(String token){
        JSONObject jsonObject= new JSONObject();
        User userToken = tokenUtil.jwtParser(token);
        User user = null;
        if(userToken.getUserId()!=null) user = userService.selectUserById(userToken.getUserId());
        if(user!=null) {
            jsonObject.put("status", "success");
            jsonObject.put("user", user);
            JSONObject res =  jsonObject.getJSONObject("user");
            res.put("token",token);
            jsonObject.put("user",res);
            return jsonObject;
        }
        else{
            jsonObject.put("status", "fail");

        }
        return jsonObject;
    }

    @RequestMapping("/updateUser")
    public JSONObject updateUser(String token,String sex,String email,String phone,String city,String birthday) throws ParseException {
        JSONObject jsonObject= new JSONObject();
        User userToken = tokenUtil.jwtParser(token);
        if(userToken!=null){
            userToken.setSex(sex);
            userToken.setEmail(email);
            userToken.setPhone(phone);
            userToken.setCity(city);
        }
        else{
            jsonObject.put("status", "fail");
            return jsonObject;
        }
        Date b;
        if(birthday.equals("null")||birthday.equals("")){
            System.out.println(1);
            b=null;
        }
        else{
          b  =dateFormat.parse(birthday);
        }
        userToken.setBirthday(b);
        userService.updateUser(userToken);
        jsonObject.put("status", "success");
        return jsonObject;
    }

    @RequestMapping("/updateAvatar")
    public JSONObject updateUserAvatar(String token,@RequestParam("file") MultipartFile multipartFile) throws IOException {
        JSONObject jsonObject = new JSONObject();
        User userToken = tokenUtil.jwtParser(token);
        if(userToken==null){
            jsonObject.put("status", "fail");
            return jsonObject;
        }
        String userName = userToken.getUserName();
        //上传类型是头像，文件夹名采用用户名
        String avatarPath = fileServeUtil.uploadServe("avatar", userName, multipartFile);
        if(userToken!=null){
          userToken.setAvatar(avatarPath);
        }
        System.out.println(userToken);
        userService.updateUserAvatar(userToken);
        jsonObject.put("status", "success");
        return jsonObject;
    }




}
