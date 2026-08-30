package com.memory.xiaohua.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.memory.xiaohua.model.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,16:55
 */
@Data
public class UserLoginVO {
    // 用户信息
    private User user;
    private String token;
}
