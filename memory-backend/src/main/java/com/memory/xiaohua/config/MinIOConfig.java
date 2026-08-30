package com.memory.xiaohua.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/10,21:30
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinIOConfig {
    private String accessKey;
    private String secretKey;
    private String url;
    private String bucketName;
    /**
     * 浏览器可访问的公网地址，用于替换预签名 URL 中的 endpoint（Docker 部署时使用）
     */
    private String publicUrl;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey,secretKey)
                .build();
    }
}
