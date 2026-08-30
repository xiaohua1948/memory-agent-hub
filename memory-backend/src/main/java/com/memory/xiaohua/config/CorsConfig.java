package com.memory.xiaohua.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口
                // 允许任意来源（本地开发 localhost:8080/8082、Docker 部署 127.0.0.1:8081/8083 等）。
                // 注意：allowCredentials(true) 时不能用 allowedOrigins("*")，必须用 allowedOriginPatterns
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的方法
                .allowedHeaders("*") // 允许所有头部
                .exposedHeaders("Authorization") // 允许前端读取 Authorization
                .allowCredentials(true) // 如果需要携带 Cookie
                .maxAge(3600); // 缓存预检请求结果（1小时）
    }
}