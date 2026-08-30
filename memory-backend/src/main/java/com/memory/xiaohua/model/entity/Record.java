package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * <p>
 * 操作记录表
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-06
 */
@Data
@TableName("record")
public class Record implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    @TableField("operation_time")
    private LocalDateTime OperationTime;


    @TableField("operation")
    private String operation;


    @TableField("number")
    private Integer number;


    @TableField("ipv4")
    private String ipv4;


    @TableField("user_id")
    private Long userId;
}
