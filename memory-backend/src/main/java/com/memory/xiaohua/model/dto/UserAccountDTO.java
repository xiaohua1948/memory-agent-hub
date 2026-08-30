package com.memory.xiaohua.model.dto;

import lombok.Data;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,14:45
 */
@Data
public class UserAccountDTO {
    // 用户账号
    private String account;

    // 用户密码
    private String password;

    private String newPassword;

    private String confirmPassword;

    // 用户邮件
    private String email;

    // 验证码
    private String code;
}
