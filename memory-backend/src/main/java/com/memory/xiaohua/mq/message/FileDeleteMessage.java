package com.memory.xiaohua.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件彻底删除消息。
 * 投递时机：用户点击"彻底删除"后，仅同步解绑 user_file，
 * 真正的 OSS 对象删除 / file 主表物理删除 / Redis 秒传缓存清理交由异步消费者完成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDeleteMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待处理（可能物理删除）的文件主表 id */
    private String fileId;

    /** 文件 md5，用于清理秒传缓存 upload:md5:{md5} */
    private String md5;

    /** OSS 原图对象名 */
    private String objectName;

    /** OSS 缩略图对象名（可为空） */
    private String thumbnailObjectName;
}
