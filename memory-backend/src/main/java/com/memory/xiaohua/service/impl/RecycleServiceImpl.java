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
        return page;
    }

    @Override
    public void recoverPicture(List<String> fileIds, Long userId) {

        userFileMapper.updateUseFile(userId, fileIds, false);
    }

    @Override
    public void dropPicture(List<String> fileIds, Long userId) {
        // 2.5 修复：selectFileByIds 查询结果从未使用，移除死代码
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
        for (FileEntity fileEntity : list) {
            String fileId = fileEntity.getFileId();
            // QueryWrapper 必须在循环内创建，否则 eq 条件会不断叠加（AND file_id='a' AND file_id='b'），
            // 导致查询永远为空，进而把仍被其他用户持有的文件物理删除
            QueryWrapper<UserFileEntity> queryWrapper = new QueryWrapper<>();
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
