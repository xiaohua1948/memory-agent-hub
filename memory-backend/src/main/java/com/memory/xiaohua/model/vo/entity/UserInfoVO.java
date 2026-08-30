package com.memory.xiaohua.model.vo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/4,17:02
 */
@Data
@TableName(value = "user")
public class UserInfoVO {
    // 用户ID
    @TableField("id")
    private Long userId;

    // 用户名称
    private String userName;

    // 用户账号
    private String account;

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


    private Long totalSpace;



    private Long usedSpace;



    private String accountStatus;


    private Integer membershipDays;
}
