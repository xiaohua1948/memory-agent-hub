package com.memory.xiaohua.utils.auth;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,16:30
 */
@Component
public class SendEmailUtil {

        @Value("${spring.mail.username}")
        private String senderEmail;

        @Resource
        private JavaMailSender javaMailSender;//    注入qq发送邮件的bean

         private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";


         private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

        public boolean validate(final String hex) {
            Matcher matcher = pattern.matcher(hex);
                     return !matcher.matches();
        }
        public String sendEmailCode(String email) {

            SimpleMailMessage message = new SimpleMailMessage();
            int codeLen = 6;
            int num = (int)((Math.random() * 9 + 1) * Math.pow(10,codeLen-1));
            // 发件人 你的邮箱
            message.setFrom(senderEmail);
            // 接收人 接收者邮箱
            message.setTo(new String[]{email});
            //邮件标题
            message.setSubject("MemorySpace账号验证码：");
            //邮件验证码
            message.setText("【Memory】验证码：<a>"+num+"</a>，用于账号验证码登录，5分钟内有效。验证码提供给他人可能导致帐号被盗，请勿泄露，谨防被骗。");

            System.out.println(javaMailSender);

            javaMailSender.send(message);
            return String.valueOf(num);
        }
}
