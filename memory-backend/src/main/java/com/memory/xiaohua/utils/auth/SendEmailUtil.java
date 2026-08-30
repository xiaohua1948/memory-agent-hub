package com.memory.xiaohua.utils.auth;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,16:30
 */
@Slf4j
@Component
public class SendEmailUtil {

        @Value("${spring.mail.username}")
        private String senderEmail;

        @Resource
        private JavaMailSender javaMailSender;

         private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";


         private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

        public boolean validate(final String hex) {
            Matcher matcher = pattern.matcher(hex);
                     return !matcher.matches();
        }
        public String sendEmailCode(String email) {
            // 2.1.4 修复：改用 SecureRandom 生成验证码（Math.random 非加密，可预测）
            SecureRandom random = new SecureRandom();
            int num = 100000 + random.nextInt(900000);  // 6 位
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(new String[]{email});
            message.setSubject("MemorySpace账号验证码：");
            message.setText("【Memory】验证码：" + num + "，用于账号验证码登录，5分钟内有效。验证码提供给他人可能导致帐号被盗，请勿泄露，谨防被骗。");
            javaMailSender.send(message);
            return String.valueOf(num);
        }
}
