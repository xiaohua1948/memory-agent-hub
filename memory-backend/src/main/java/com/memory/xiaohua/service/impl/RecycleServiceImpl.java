package com.memory.xiaohua.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.UserFileMapper;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.UserFileEntity;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.mq.FileDeleteProducer;
import com.memory.xiaohua.mq.message.FileDeleteMessage;
import com.memory.xiaohua.service.RecycleService;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/5,19:44
 */
@Service
public class RecycleServiceImpl implements RecycleService {
    @Resource
    FileMapper fileMapper;
    @Resource
    UserFileMapper userFileMapper;

    @Resource
    MinioOSSUtil minioOSSUtil;

    @Resource
    FileDeleteProducer fileDeleteProducer;

    @Override
    public Page<FileInfoListVO> getFileInfoList(Integer current, Integer size, String orderType, String orderKeyword, String imageTypeText, Long userId) {
        Page<FileInfoListVO> page = new Page<>(current, size);
        List<FileInfoListVO> fileInfoListVOS = fileMapper.getFileInfoList(page, orderType, orderKeyword, imageTypeText, null, null, userId, null, true);
        page.setRecords(fileInfoListVOS);
        return page;
    }

    @Override
    public void recoverPicture(List<String> fileIds, Long userId) {

        userFileMapper.updateUseFile(userId, fileIds, false);
    }

    @Override
    public void dropPicture(List<String> fileIds, Long userId) {
        // 2.5 修复：selectFileByIds 查询结果从未使用，移除死代码
        // 1. 同步解绑当前用户的 user_file 关联（用户视角立即"已删除"）
        userFileMapper.dropFile(userId, fileIds);
        // 2. 异步投递彻底删除消息：若无人持有则物理删除 file 主表 + OSS 对象 + 清理秒传缓存；
        //    仍有人持有则仅清理秒传缓存（保留对象供其他账号复用）
        for (String fileId : fileIds) {
            FileEntity entity = fileMapper.selectById(fileId);
            if (entity == null) {
                continue;
            }
            fileDeleteProducer.send(FileDeleteMessage.builder()
                    .fileId(entity.getFileId())
                    .md5(entity.getMd5())
                    .objectName(entity.getFileObjectName())
                    .thumbnailObjectName(entity.getThumbnailObjectName())
                    .build());
        }
    }

    @Override
    public void cronDropPicture() {
        List<FileEntity> list = userFileMapper.selectExpiredPicture();
        if (list.isEmpty()) {
            return;
        }
        List<String> fileIds = new ArrayList<>();
        for (FileEntity fileEntity : list) {
            fileIds.add(fileEntity.getFileId());
        }
        // 删除过期关联表
        userFileMapper.dropAllFile(fileIds);
        // 对每个过期文件：先解绑，再异步投递彻底删除消息（由消费者判定是否物理删除）
        for (FileEntity fileEntity : list) {
            fileDeleteProducer.send(FileDeleteMessage.builder()
                    .fileId(fileEntity.getFileId())
                    .md5(fileEntity.getMd5())
                    .objectName(fileEntity.getFileObjectName())
                    .thumbnailObjectName(fileEntity.getThumbnailObjectName())
                    .build());
        }
    }


}
