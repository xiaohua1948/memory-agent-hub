package com.memory.xiaohua.model.vo.album;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/8,1:32
 */
@Data
public class PersonAlbumVO {
    private Long personId;
    private Long faceId;
    private String personName;
    private String personRelation;
    private String coverUrl;
    private Long total;
    private LocalDateTime createTime;
}
