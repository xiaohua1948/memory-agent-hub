package com.memory.xiaohua.model.vo;

import com.memory.xiaohua.model.vo.entity.FileInfo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/21,0:17
 */
@Data
public class FileInfoListVO {
    private LocalDate Time;
    private List<FileInfo> fileList;
}
