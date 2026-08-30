package com.memory.xiaohua.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.memory.xiaohua.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-18
 */
@Service
public interface UserService extends IService<User> {

    void updateUserAvatar(MultipartFile multipartFile,Long userId,String suffix);
}
