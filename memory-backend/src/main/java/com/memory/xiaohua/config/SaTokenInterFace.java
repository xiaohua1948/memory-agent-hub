package com.memory.xiaohua.config;

import cn.dev33.satoken.stp.StpInterface;
import com.memory.xiaohua.mapper.UserMapper;
import com.memory.xiaohua.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-24,23:03
 */
@Component
public class SaTokenInterFace implements StpInterface {

    @Autowired
    private UserMapper userMapper; // 假设你的 UserMapper 可以查询用户角色

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 1. 根据用户ID查询用户信息（从数据库）
        Long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        // 2. 提取角色信息（假设 user.getRole() 返回 "admin" 或 "user"）
        List<String> roleList = new ArrayList<>();
        roleList.add(user.getRole()); // 将用户角色添加到列表
        return roleList;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 根据角色返回权限列表（可选）
        return new ArrayList<>();
    }

}
