package com.memory.xiaohua.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.RecordMapper;
import com.memory.xiaohua.mapper.UserMapper;
import com.memory.xiaohua.mapper.UserStorageMapper;
import com.memory.xiaohua.model.dto.PageRequest;
import com.memory.xiaohua.model.dto.UserInfoDTO;
import com.memory.xiaohua.model.entity.Record;
import com.memory.xiaohua.model.entity.User;
import com.memory.xiaohua.model.entity.UserStorage;
import com.memory.xiaohua.model.vo.entity.UserInfoVO;
import com.memory.xiaohua.model.vo.visual.MemoryVO;
import com.memory.xiaohua.utils.auth.PasswordUtil;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-24,09:22
 */
@RestController
@RequestMapping("/admin")
public class AdminController {


    @Resource
    UserMapper userMapper;

    @Resource
    UserStorageMapper userStorageMapper;

    @Resource
    RecordMapper recordMapper;

    @Resource
    PasswordUtil passwordUtil;

    @SaCheckRole("admin")
    @GetMapping("/selectAllUserInfo")
    public BaseResponse<?> selectAllUserInfo() {

        List<UserInfoVO> users = userMapper.selectAllUserInfo("user");
        return ResultUtil.success(users,"查询成功");
    }

    @SaCheckRole("admin")
    @GetMapping("/selectAllAdmin")
    public BaseResponse<?> selectAllAdmin() {

        List<UserInfoVO> admin = userMapper.selectAllAdmin("admin");
        return ResultUtil.success(admin,"查询成功");
    }

    @SaCheckRole("admin")
    @PostMapping("/selectAllRecord")
    public BaseResponse<?> selectAllRecord(@RequestBody PageRequest pageRequest) {
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        IPage<Record> iPage = new Page<>();
        iPage.setCurrent(pageRequest.getCurrent());
        iPage.setSize(pageRequest.getPageSize());

        List<Record> records = recordMapper.selectList(iPage,queryWrapper);

        iPage.setRecords(records);
        return ResultUtil.success(iPage,"查询成功");
    }

    @SaCheckRole("admin")
    @PostMapping("/updateUserInfo")
    @Transactional
    public BaseResponse<?> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO) {
        UpdateWrapper<User>  updateWrapper1 = new UpdateWrapper<>();
        updateWrapper1.eq("id",userInfoDTO.getUserId());
        updateWrapper1.set("user_name",userInfoDTO.getUserName());
        updateWrapper1.set("profile",userInfoDTO.getProfile());
        userMapper.update(updateWrapper1);

        UpdateWrapper<UserStorage> updateWrapper2 = new UpdateWrapper<>();
        updateWrapper2.eq("user_id",userInfoDTO.getUserId());
        updateWrapper2.set("account_status",userInfoDTO.getAccountStatus());
        updateWrapper2.set("membership_days",userInfoDTO.getMembershipDays());
        updateWrapper2.set("total_space",userInfoDTO.getTotalSpace());
        userStorageMapper.update(updateWrapper2);
        return ResultUtil.success("修改成功");
    }

    @SaCheckRole("admin")
    @PostMapping("/addAdmin")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<?> addAdmin(@RequestBody User userDto) {
        if (userDto == null || userDto.getAccount() == null || userDto.getPassword() == null || userDto.getEmail() == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数不能为空!");
        }
        // 2.7 修复：检查账号/邮箱重复（避免重复插入报 500）
        QueryWrapper<User> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("account", userDto.getAccount())
                .or().eq("email", userDto.getEmail());
        if (userMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "账号或邮箱已存在!");
        }
        User admin = new User();
        // 2.7 修复：Math.random()*100 生成小数用户名，改为整数
        int suffix = (int) (Math.random() * 100);
        admin.setUserName("管理员" + suffix);
        admin.setAccount(userDto.getAccount());
        // 2.7 修复：复用注入的 PasswordUtil，而非每次 new BCryptPasswordEncoder()
        admin.setPassword(passwordUtil.encode(userDto.getPassword()));
        admin.setRole("admin");
        admin.setEmail(userDto.getEmail());
        userMapper.insert(admin);
        return ResultUtil.success("添加成功");
    }

    @SaCheckRole("admin")
    @GetMapping("/selectVisualData")
    public BaseResponse<?> selectVisualData(){
        MemoryVO memoryVO = recordMapper.selectOperationStats("上传", "下载");
        Long l = userMapper.countNormalUsers();
        memoryVO.setUserCount(l);
        return ResultUtil.success(memoryVO,"查询成功");
    }
}
