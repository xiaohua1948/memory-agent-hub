package com.memory.xiaohua.model.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 照片打包下载请求
 *
 * @author xiaohua
 */
@Data
public class AiDownloadZipRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 要打包下载的照片 fileId 列表 */
    private List<String> fileIds;
}
