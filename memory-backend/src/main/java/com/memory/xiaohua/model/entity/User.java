package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @description: 用户实体类
 * @author: xiaohua
 * @date: 2025/2/8,15:25
 */
@Data
@TableName(value = "user")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    //userId
    @TableId(value = "id", type = IdType.AUTO)
    private Long userId;

    // 用户名称
    private String userName;

    // 用户账号
    private String account;

    // 用户密码
    private String password;

    // 用户邮件
    private String email;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 用户简介
    private String profile;

    // 头像URL
    private String avatarUrl;

    //头像存储桶
    private String avatarObjectName;
}