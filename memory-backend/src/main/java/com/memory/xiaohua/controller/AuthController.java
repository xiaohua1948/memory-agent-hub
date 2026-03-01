package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.dto.UserAccountDTO;
import com.memory.xiaohua.model.vo.UserLoginVO;
import com.memory.xiaohua.service.AuthService;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.auth.SendEmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,16:21
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SendEmailUtil sendEmailUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private RedisUtil redisUtil;


    @RequestMapping("/accountRegister")
    public BaseResponse<?> accountRegister(@RequestBody UserAccountDTO userAccountDTO) {
        Boolean res = authService.accountRegister(userAccountDTO);
        if(res){
            return ResultUtil.success("账户注册成功,请登录!");
        }
        else {
            return ResultUtil.error(StatusCode.PARAMS_ERROR.getCode(),"账户注册失败！");
        }

    }


    @RequestMapping("/accountLogin")
    public BaseResponse<UserLoginVO> accountLogin(@RequestBody UserAccountDTO userAccountDTO)  {
        UserLoginVO userLoginVO = authService.accountLogin(userAccountDTO);

        return ResultUtil.success(userLoginVO,"账户登录成功");
    }

    @RequestMapping("/getEmailCode")
    public BaseResponse<?> sendEmailCode(@RequestBody UserAccountDTO userAccountDTO)  {
        String emailCode = authService.getEmailCode(userAccountDTO);

        return ResultUtil.success("验证码已发送至："+ userAccountDTO.getEmail());
    }

    @RequestMapping("/codeLogin")
    public BaseResponse<UserLoginVO> codeLogin(@RequestBody UserAccountDTO userAccountDTO)  {
        UserLoginVO userLoginVO = authService.codeLogin(userAccountDTO);
        return ResultUtil.success(userLoginVO,"账户登录成功!");
    }

    @RequestMapping("/accountLogout")
    public BaseResponse<UserLoginVO> accountLogout() {
        StpUtil.logout();
        return ResultUtil.success(null,"账户退出登录!");
    }

    @RequestMapping("/updatePassWord")
    public BaseResponse<?> updatePassWord(@RequestBody UserAccountDTO userAccountDTO)  {
        authService.updatePassword(userAccountDTO);
        return ResultUtil.success("账户密码修改成功!");
    }

    @RequestMapping("/updateEmail")
    public BaseResponse<?> updateEmail(@RequestBody UserAccountDTO userAccountDTO)  {
        authService.updateEmail(userAccountDTO);
        return ResultUtil.success("邮箱修改成功!");
    }
}
