package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;


/**
 * <p>
 * 
 * </p>
 *
 * @author xiaohua
 * @since 2025-04-07
 */
@Data
@TableName("similar_picture")
public class SimilarPicture implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("similar_id")
    private String similarId;

    @TableField("file_id")
    private String fileId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("user_id")
    private Long userId;
}
