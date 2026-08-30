package com.memory.xiaohua.utils.auth;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/20,15:11
 */
@Component
public class PasswordUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // 加密密码
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 验证密码
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}