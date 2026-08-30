package com.memory.xiaohua.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/23,20:48
 */
@Configuration
public class SwaggerConfig {
    /*
    配置swagger基本信息
     */
    @Bean
    public OpenAPI swaggerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Memory接口文档")
                        .description("Swagger简单入门")
                        .version("v1.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("网站"));
    }
}