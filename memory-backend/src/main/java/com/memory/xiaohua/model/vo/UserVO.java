package com.memory.xiaohua.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息 VO（对外返回用）
 * 与实体 User 相比剔除了 password（密码哈希）、avatarObjectName（MinIO 内部对象名）等敏感/内部字段，
 * 避免登录接口将密码哈希下发到前端。
 *
 * @description:
 * @author: xiaohua
 * @date: 2026/8/31
 */
@Data
public class UserVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    //userId
    private Long userId;

    // 用户名称
    private String userName;

    // 用户账号
    private String account;

    //用户角色
    private String role;

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
}
