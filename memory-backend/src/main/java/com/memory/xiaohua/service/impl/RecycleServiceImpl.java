package com.memory.xiaohua.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.UserFileMapper;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.UserFileEntity;
import com.memory.xiaohua.model.vo.FileInfoListVO;
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

    @Override
    public Page<FileInfoListVO> getFileInfoList(Integer current, Integer size, String orderType, String orderKeyword, String imageTypeText, Long userId) {
        Page<FileInfoListVO> page = new Page<>(current, size);
        List<FileInfoListVO> fileInfoListVOS = fileMapper.getFileInfoList(page, orderType, orderKeyword, imageTypeText, null, null, userId, null, true);
        page.setRecords(fileInfoListVOS);
        System.out.println(fileInfoListVOS);
        return page;
    }

    @Override
    public void recoverPicture(List<String> fileIds, Long userId) {

        userFileMapper.updateUseFile(userId, fileIds, false);
    }

    @Override
    public void dropPicture(List<String> fileIds, Long userId) {
        List<FileEntity> fileEntities = fileMapper.selectFileByIds(fileIds, userId);
        //只删除关联表，物理删除，等服务器定时任务执行
        userFileMapper.dropFile(userId, fileIds);
    }

    @Override
    public void cronDropPicture() {
        List<FileEntity> list = userFileMapper.selectExpiredPicture();
        List<String> fileIds = new ArrayList<>();
        if(list.isEmpty()){
            return;
        }
        for (FileEntity fileEntity : list) {
            fileIds.add(fileEntity.getFileId());
        }
        //删除关联表
        userFileMapper.dropAllFile(fileIds);
        QueryWrapper<UserFileEntity> queryWrapper = new QueryWrapper<>();
        for (FileEntity fileEntity : list) {
            String fileId = fileEntity.getFileId();
            queryWrapper.eq("file_id",fileId);
            List<UserFileEntity> userFileEntities = userFileMapper.selectList(queryWrapper);
            if(userFileEntities.isEmpty()){
                QueryWrapper<FileEntity> delete = new QueryWrapper<>();
                delete.eq("file_id",fileId);
                fileMapper.delete(delete);
                //如果数据库没有该文件id的关联， 就彻底删掉缩略图和原文件
                minioOSSUtil.delete(fileEntity.getFileObjectName());
                minioOSSUtil.delete(fileEntity.getThumbnailObjectName());
            }

        }
    }


}
