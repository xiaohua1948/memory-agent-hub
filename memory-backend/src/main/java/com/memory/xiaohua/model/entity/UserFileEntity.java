package com.memory.xiaohua.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * <p>
 * 用户-文件关联表
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-27
 */
@Data
@TableName("user_file")

public class UserFileEntity implements Serializable {


    @TableField("id")
    private Long id;

    @TableField("user_id")
    private Long userId;


    @TableField("file_id")
    private String fileId;


    @TableField("is_deleted")
    private Boolean isDeleted;


    @TableField("upload_time")
    private LocalDateTime uploadTime;


    @TableField("deleted_time")
    private LocalDateTime deletedTime;


    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
