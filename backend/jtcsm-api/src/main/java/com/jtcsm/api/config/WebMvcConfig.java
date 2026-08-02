package com.jtcsm.api.config;

import com.jtcsm.common.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 小程序 API 模块 —— Web MVC 配置
 * <p>
 * 注册 JWT 拦截器，对所有 /api/v1/** 路径进行鉴权（白名单路径在 application.yml 中配置）。
 * </p>
 */
/**
 * Web MVC 配置 —— 注册 JWT 拦截器
 * <p>
 * 对所有 /api/v1/** 路径进行鉴权（白名单路径在 application.yml 中配置）
 * </p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 JWT 拦截器，拦截所有 /api/v1/** 请求
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
