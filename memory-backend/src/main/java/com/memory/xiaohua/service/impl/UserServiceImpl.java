package com.memory.xiaohua.service.impl;


import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.memory.xiaohua.mapper.UserMapper;
import com.memory.xiaohua.model.entity.User;
import com.memory.xiaohua.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memory.xiaohua.utils.file.FileUtil;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-18
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private MinioOSSUtil minioOSSUtil;

    @Resource
    private FileUtil fileUtil;

    @Resource
    private UserMapper userMapper;

    @Override
    public void updateUserAvatar(MultipartFile multipartFile, Long userId,String suffix) {
        InputStream inputStream1 = null;
        InputStream inputStream2 = null;
        String md5 ="";
        try {
            inputStream2 = multipartFile.getInputStream();
            inputStream1 = multipartFile.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String contentType = multipartFile.getContentType();
        long size = multipartFile.getSize();

        md5 = fileUtil.getMD5(inputStream2);
        String objectName = "avatar/"+userId+"/"+userId+suffix;
        minioOSSUtil.uploadToOSS(objectName,inputStream1,size,contentType);
        String fileUrl = minioOSSUtil.getFileUrl(objectName);

        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",userId);
        updateWrapper.set("avatar_url",fileUrl);
        updateWrapper.set("avatar_object_name",objectName);
        userMapper.update(updateWrapper);
    }
}
