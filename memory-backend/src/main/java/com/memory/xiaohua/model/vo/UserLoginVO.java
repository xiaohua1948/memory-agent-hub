package com.memory.xiaohua.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.memory.xiaohua.model.vo.UserVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,16:55
 */
@Data
public class UserLoginVO {
    // 用户信息（脱敏 VO，不含密码哈希等敏感字段）
    private UserVO user;
    private String token;
}
