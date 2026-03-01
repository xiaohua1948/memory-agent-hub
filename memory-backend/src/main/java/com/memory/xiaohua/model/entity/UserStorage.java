package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>
 * 用户存储信息表
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-10
 */
@Data
@TableName("user_storage")
public class UserStorage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("user_id")
    private Long userId;

    @TableField("total_space")
    private Long totalSpace;


    @TableField("used_space")
    private Long usedSpace;


    @TableField("account_status")
    private String accountStatus;

    @TableField("membership_days")
    private Integer membershipDays;
}
