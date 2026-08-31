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
    /**
     * MinIO 区域，默认 us-east-1（MinIO 默认值）。
     * 必须显式指定：SDK 生成预签名 URL 前若拿不到 region，会真实发起 GetBucketLocation 请求探测，
     * 当 endpoint 是 public-url（容器内往往不可达，如 http://127.0.0.1:9002）时会直接 Connection refused。
     * 指定 region 后签名纯本地计算，不再发网络请求。
     */
    private String region = "us-east-1";

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey,secretKey)
                .build();
    }
}
