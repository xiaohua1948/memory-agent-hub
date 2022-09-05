package com.xiaohua.service;

import com.xiaohua.domain.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    /**
     * 注册用户
     * @param user
     * @return
     */
    Integer addUser(User user);

    /**
     * 用户登录
     * @param user
     * @return
     */
    Integer selectUserId(User user);

    String selectUserName(String UserName);
    /**
     * 查询用户资料
     * @param id
     * @return
     */
    User selectUserById(int id);

    /**
     * 修改用户资料
     * @param user
     */
    void updateUser(User user);

    /**
     * 修改用户头像
     * @param user
     */
    void updateUserAvatar(User user);



}
