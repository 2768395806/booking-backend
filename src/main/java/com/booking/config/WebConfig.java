package com.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：CORS + 商家后台鉴权拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**", "/api/platform/**")
                .excludePathPatterns("/api/admin/login");
        // 登录/下单/申请/评论/收藏 防刷（拦截器内部按 POST + URI 决定是否限流）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/orders/**", "/api/merchant/apply", "/api/reviews/**",
                        "/api/wx/login", "/api/wx/bind", "/api/favorites/toggle", "/api/admin/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 上传图片静态资源映射：/uploads/** -> 上传目录（可用 UPLOAD_DIR 环境变量指向挂载盘，默认项目目录 uploads/） */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadRoot = System.getenv("UPLOAD_DIR");
        java.nio.file.Path root = (uploadRoot != null && !uploadRoot.isBlank())
                ? java.nio.file.Paths.get(uploadRoot)
                : java.nio.file.Paths.get("uploads");
        String uploadPath = root.toAbsolutePath().normalize().toUri().toString();
        if (!uploadPath.endsWith("/")) uploadPath += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath);
    }
}
