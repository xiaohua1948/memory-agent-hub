package com.memory.xiaohua.model.vo.picture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-07,16:40
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagResult {
    private String imageType;
    private String tagName;
    private Double confidence;

}
