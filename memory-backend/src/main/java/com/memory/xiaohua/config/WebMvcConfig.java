package com.memory.xiaohua.config;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/7,19:53
 */

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.router.SaRouterStaff;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor().setAuth(handle -> {
                    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                        return; // OPTIONS 请求直接放行
                    }
                    StpUtil.checkLogin(); // 否则检查登录状态
                }))
                .addPathPatterns("/**")
                .order(Ordered.HIGHEST_PRECEDENCE)
                .excludePathPatterns(
                        "/auth/accountLogin",
                        "/auth/codeLogin",
                        "/auth/getEmailCode",
                        "/auth/accountRegister",
                        "/file/getShareFileInfo",
                        "/file/selectMetaDataByFileId",
                        "/file/downloadFileByToken",
                        "/album/downloadAlbumByToken",
                        "/swagger-ui/**"
                );
    }
}