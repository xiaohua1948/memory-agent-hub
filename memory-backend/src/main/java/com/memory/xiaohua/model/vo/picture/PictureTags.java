package com.memory.xiaohua.model.vo.picture;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-07,16:41
 */
@Data
public class PictureTags {
    private String fileId;
    private List<TagResult> tags;
}
