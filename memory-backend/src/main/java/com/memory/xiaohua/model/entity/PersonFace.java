package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 人物脸部关联表
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-10
 */
@Data
@TableName("person_face")
public class PersonFace implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("person_id")
    private Long personId;

    @TableField("face_id")
    private Long faceId;

    @TableField("user_id")
    private Long userId;

    @TableField("representative")
    private Boolean representative;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
