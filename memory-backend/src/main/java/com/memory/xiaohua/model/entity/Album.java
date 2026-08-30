package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @description: 相册实体类
 * @author: xiaohua
 * @date: 2025/2/19,20:42
 */
@Data
@TableName("album")
public class Album  implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 相册ID（雪花算法生成）
     */
    @TableId(type = IdType.AUTO)
    private Long albumId;

    /**
     * 外键用户ID
     */
    private Long userId;

    /**
     * 相册名称
     */
    private String albumName;
    

    /**
     * 相册描述
     */
    private String description;

    /**
     * 创建时间（服务器时间）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（服务器时间）
     */
    private LocalDateTime updateTime;

    /**
     * 相册状态（0正常 1公开 2私密）
     */
    private Integer status;

}