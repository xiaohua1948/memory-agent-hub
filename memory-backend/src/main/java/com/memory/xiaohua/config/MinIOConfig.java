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

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey,secretKey)
                .build();
    }
}
