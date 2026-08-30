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
 * 人像特征表
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
@Data
@TableName("face")
public class Face implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    //人像唯一标识
    @TableId(value = "face_id", type = IdType.AUTO)
    private Long faceId;


   //"文件标识(36位UUID)")
    @TableField("file_id")
    private String fileId;
    //用户id)
    @TableField("user_id")
    private Long userId;

    //("是否已处理")
    @TableField("is_processed")
    private Boolean isProcessed;

    //("128维特征向量(BLOB存储float数组)")
    @TableField("feature_vector")
    private byte[] featureVector;

   //("封面图URL")
    @TableField("person_cover_url")
    private String personCoverUrl;

    //("对象名称")
    @TableField("person_object_name")
    private String personObjectName;

    //("创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("is_face")
    private Boolean isFace;
}
