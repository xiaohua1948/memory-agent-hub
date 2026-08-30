package com.memory.xiaohua.model.vo.entity;

import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.ImageMetaData;
import com.memory.xiaohua.model.entity.VideoMetaData;
import lombok.Data;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/28,20:33
 */
@Data
public class FileMetaDataVO {
    //文件基本信息
    FileEntity fileInfo;
    //图片媒体信息
    ImageMetaData imageMetaData;
    //视频媒体信息
    VideoMetaData videoMetaData;
}
