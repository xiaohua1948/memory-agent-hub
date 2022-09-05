package com.xiaohua.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class UploadConfig implements WebMvcConfigurer {

    @Value("${file.upload.imgPath}")
    private String uploadPath;
    //配置本地文件映射到url上
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //重写方法
        //修改tomcat 虚拟映射
        registry.addResourceHandler("/serve/**")
                .addResourceLocations("file:"+uploadPath);//定义图片存放路径

        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        System.out.println(uploadPath);
    }
}