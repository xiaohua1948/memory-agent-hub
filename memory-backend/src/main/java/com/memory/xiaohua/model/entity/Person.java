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
 * <p>
 * 人物信息表
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
@Data
@TableName("person")

public class Person implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    //("人物唯一标识")
    @TableId(value = "person_id", type = IdType.AUTO)
    private Long personId;

    @TableField("user_id")
    private Long userId;

    //("人物姓名")
    @TableField("person_name")
    private String personName;

    //("人物关系(如：父子/同事)")
    @TableField("person_relation")
    private String personRelation;

    //("创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    //是否隐藏
    @TableField("display")
    private Boolean display;
}
