package com.memory.xiaohua.model.vo.visual;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/5,23:55
 */
@Data
public class VisualFileVO {

    List<FileContentType> typeData;

    List<FileSize> sizeData;

    Long imageCount;

    Long videoCount;

}
