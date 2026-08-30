package com.memory.xiaohua.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/9,23:05
 */
@Data
public class PersonDTO {
    private Long personId;

    private String personName;

    private Long faceId;

    private Long oldFaceId;

    private Long userId;

    private String personRelation;

    private LocalDateTime createTime;

}
