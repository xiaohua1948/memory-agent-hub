package com.memory.xiaohua.config;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-24,10:44
 */
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.memory.xiaohua.model.entity.User;
import com.memory.xiaohua.service.impl.UserServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class AdminInitializer implements CommandLineRunner {

    @Resource
    private UserServiceImpl userService; // 假设你有UserRepository

    @Override
    public void run(String... args) throws Exception {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account","admin");
        queryWrapper.eq("role","admin");
        User res = userService.getOne(queryWrapper);
        if (res == null) {
            User admin = new User();
            admin.setUserName("管理员");
            admin.setAccount("admin");
            admin.setPassword(new BCryptPasswordEncoder().encode("12345678"));
            admin.setRole("admin");
            admin.setEmail("admin@163.com");
            userService.save(admin);
        }
    }
}