package com.memory.xiaohua.model.vo.album;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/24,21:27
 */
@Data
public class AlbumVO {
    /**
     * 相册ID（雪花算法生成）
     */
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
     * 封面
     */
    private String coverUrl;

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
     * 照片数量（冗余计数）
     */
    private Integer imageCount;
    private Integer videoCount;
}
