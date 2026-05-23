package com.funfun.schedule.config;

import com.funfun.schedule.interceptor.AuthInterceptor;
import com.funfun.schedule.interceptor.OpenApiAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web配置类
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {
    
    // 由于ResponseAdvice已经使用@RestControllerAdvice注解，
    // 会被Spring自动扫描到，所以这里不需要额外配置拦截器
    
    // 如有其他Web相关配置，可以在这里添加

    @Autowired
    CorsConfig corsConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("getAllowedOriginPatterns:{}",corsConfig.getAllowedOriginPatterns());
        registry.addMapping("/**") // 允许所有接口跨域
                .allowedOriginPatterns(corsConfig.getAllowedOriginPatterns().toArray(new String[0]))                // 生产环境需指定真实前端域名，如 "https://xxx.com"，多个用逗号分隔
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头（如 Token、Content-Type）
                .allowCredentials(true) // 允许携带 Cookie（如需登录态共享）
                .maxAge(3600); // 预检请求（OPTIONS）缓存时间（1小时）
    }

    @Autowired
    private AuthInterceptor jwtAuthInterceptor;

    @Autowired
    private OpenApiAuthInterceptor openApiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/login", "/api/public/**");
        // 开放接口（OpenAPI / MCP）：独立的 Bearer Token 鉴权链路，token 绑定 groupId。
        registry.addInterceptor(openApiAuthInterceptor)
                .addPathPatterns("/openapi/**");
    }
}