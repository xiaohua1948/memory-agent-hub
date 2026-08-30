package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.UserStorageMapper;
import com.memory.xiaohua.model.dto.UserInfoDTO;
import com.memory.xiaohua.model.entity.User;
import com.memory.xiaohua.model.entity.UserStorage;
import com.memory.xiaohua.model.vo.entity.UserInfoVO;
import com.memory.xiaohua.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    UserService userService;

    @Resource
    UserStorageMapper userStorageMapper;

    @PostMapping("/updateUserInfo")
    public BaseResponse<?> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO) throws InterruptedException {
        UpdateWrapper<User> updateWrapper= new UpdateWrapper<>();
        updateWrapper.set("user_name",userInfoDTO.getUserName());
        updateWrapper.set("profile",userInfoDTO.getProfile());
        userService.update(updateWrapper);
        return ResultUtil.success("用户信息修改成功!");
    }

    @RequestMapping("/getUserInfo")
    public BaseResponse<UserInfoVO> getUserInfo()  {
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>();
        queryWrapper.eq("id",userId);
        User one = userService.getOne(queryWrapper);


        UserInfoVO userInfoVO = new UserInfoVO();
        if (one.getRole().equals("user")){
            UserStorage userStorage = userStorageMapper.selectById(userId);
            BeanUtils.copyProperties(userStorage, userInfoVO);
        }
        BeanUtils.copyProperties(one, userInfoVO);
        return ResultUtil.success(userInfoVO,"用户信息获取成功!");
    }

    @PostMapping("/uploadAvatar")
    public BaseResponse<?> updateUserAvatar(MultipartFile file) {
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        String originalFilename = file.getOriginalFilename();
        int i = originalFilename.lastIndexOf('.');
        String suffix = originalFilename.substring(i);
        userService.updateUserAvatar(file,userId,suffix);
        return ResultUtil.success("用户头像修改成功!");
    }

    @PostMapping("/updateUserStatus")
    public BaseResponse<?> handleUpdateUserStatus(String status,Integer day) {

        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        UserStorage userStorage = userStorageMapper.selectById(userId);
        UpdateWrapper<UserStorage>  updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id",userId);
        updateWrapper.set("membership_days",userStorage.getMembershipDays()+day);
        updateWrapper.set("account_status",status);
        userStorageMapper.update(updateWrapper);
        return ResultUtil.success("用户头像修改成功!");
    }

    @PostMapping("/addUserStorage")
    public BaseResponse<?> addUserStorage(Long size){
        Long maxSpace = 21474836480l;
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        UserStorage userStorage = userStorageMapper.selectById(userId);
        Long totalSpace = userStorage.getTotalSpace();
        Long newTotalSpace = totalSpace+size;
        if(newTotalSpace > maxSpace){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "扩容失败，超过最大容量!");
        }

        UpdateWrapper<UserStorage>  updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id",userId);
        updateWrapper.set("total_space",newTotalSpace);
        userStorageMapper.update(updateWrapper);
        return ResultUtil.success("添加容量成功!");
    }

}
