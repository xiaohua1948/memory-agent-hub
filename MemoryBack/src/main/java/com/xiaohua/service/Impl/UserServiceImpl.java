package com.xiaohua.service.Impl;

import com.xiaohua.domain.User;
import com.xiaohua.dao.UserMapper;
import com.xiaohua.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    /**
     * 用户注册
     * @param user
     * @return
     */
    @Override
    public Integer addUser(User user) {
        Integer userId = userMapper.add(user);
        return userId;
    }

    /**
     * 查询用户id
     * @param user
     * @return
     */
    @Override
    public Integer selectUserId(User user) {
        Integer userId = userMapper.selectUserId(user);
        return userId;
    }

    /**
     * 查询用户名
     * @param UserName
     * @return
     */
    @Override
    public String selectUserName(String UserName) {

        return userMapper.selectUserName(UserName);
    }

    /**
     * 查询用户
     * @param id
     * @return
     */
    @Override
    public User selectUserById(int id) {
        User user = userMapper.selectUserById(id);
        return user;
    }

    /**
     * 用户资料更新
     * @param user
     */
    @Override
    public void updateUser(User user) {
         userMapper.updateUser(user);
    }

    /**
     * 用户头像更新
     * @param user
     */
    @Override
    public void updateUserAvatar(User user) {
        userMapper.updateUserAvatar(user);
    }

}
