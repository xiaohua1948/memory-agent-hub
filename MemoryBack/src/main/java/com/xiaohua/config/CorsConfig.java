package com.xiaohua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

//跨域问题解决(全局跨域)
@Configuration
public class CorsConfig {

    // 当前跨域请求最大有效时长。这里默认1天
    private static final long MAX_AGE = 24 * 60 * 60;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowCredentials(true);
        //允许所有域名进行跨域调用
        corsConfiguration.addAllowedOriginPattern("*");
        //允许所有请求头
        corsConfiguration.addAllowedHeader("*");
        //允许所有方法
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setMaxAge(MAX_AGE);
        //  对接口配置跨域设置
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(source);
    }
}