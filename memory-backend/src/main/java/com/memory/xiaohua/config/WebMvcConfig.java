package com.memory.xiaohua.config;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/7,19:53
 */

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {



    /**
     * 配置saToken的身份权限拦截器
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 建议改用过滤器方式配置

        registry.addInterceptor(new SaInterceptor().setAuth(handle -> StpUtil.checkLogin()))
                // 将所有路径都拦截
                .addPathPatterns("/**")
                // 显式设置拦截器优先级
                .order(Ordered.HIGHEST_PRECEDENCE)
                // 排除不拦截的路径
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
