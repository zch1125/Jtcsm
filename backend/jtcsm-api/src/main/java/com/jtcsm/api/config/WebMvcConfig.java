package com.jtcsm.api.config;

import com.jtcsm.common.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

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

    /** 菜谱成品图目录，默认指向仓库 docs/recipes/images（开发环境从 backend/jtcsm-api 启动） */
    @Value("${recipe.image-dir:../../docs/recipes/images}")
    private String recipeImageDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 JWT 拦截器，拦截所有 /api/v1/** 请求
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = resolveImageDir(recipeImageDir);
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/static/recipes/**")
                .addResourceLocations("file:" + location);
    }

    /** 兼容从仓库根目录或 backend/jtcsm-api 子目录启动，也支持容器内绝对路径 */
    private String resolveImageDir(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute() || Files.isDirectory(path)) {
            return path.toAbsolutePath().normalize().toString();
        }

        Path current = Path.of("").toAbsolutePath().normalize();
        Path resolved = current.resolve(configured).normalize();
        if (Files.isDirectory(resolved)) {
            return resolved.toString();
        }

        // 从当前目录向上查找仓库的 docs/recipes/images
        while (current != null) {
            Path repoImages = current.resolve("docs/recipes/images").normalize();
            if (Files.isDirectory(repoImages)) {
                return repoImages.toString();
            }
            current = current.getParent();
        }
        return resolved.toString();
    }
}
