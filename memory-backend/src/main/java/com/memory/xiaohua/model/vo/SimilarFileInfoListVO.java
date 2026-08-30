package com.memory.xiaohua.model.vo;

import com.memory.xiaohua.model.vo.entity.FileInfo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-07,01:51
 */
@Data
public class SimilarFileInfoListVO {
    private String similarId;
    private List<FileInfo> fileList;
}
