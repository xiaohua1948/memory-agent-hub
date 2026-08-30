package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-04
 */
@Data
@TableName("picture_tag")
public class PictureTag implements Serializable {



    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_id")
    private String fileId;

    @TableField("image_type")
    private String imageType;

    @TableField("tag_name")
    private String tagName;
}
