package com.xiaohua.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/*用@autowired注入JavaMailSender后，表示整个类交给了spring管理（类上加@Component ），在其他地方调用的时候，也应该从spring容器中获取，而不能用new去实例化
* */
@Component
public class SendEmailUtil {

    @Value("${spring.mail.username}")
    private String senderEmail;
    @Resource
    private JavaMailSender javaMailSender;//    注入qq发送邮件的bean

    public String sendEmail(String email) {
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
