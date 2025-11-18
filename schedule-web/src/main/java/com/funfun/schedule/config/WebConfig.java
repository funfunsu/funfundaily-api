package com.funfun.schedule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    // 由于ResponseAdvice已经使用@RestControllerAdvice注解，
    // 会被Spring自动扫描到，所以这里不需要额外配置拦截器
    
    // 如有其他Web相关配置，可以在这里添加
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有接口跨域
                .allowedOrigins("http://localhost:5173") // 允许的前端域名（UniApp H5 本地默认端口 8081）
                // 生产环境需指定真实前端域名，如 "https://xxx.com"，多个用逗号分隔
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头（如 Token、Content-Type）
                .allowCredentials(true) // 允许携带 Cookie（如需登录态共享）
                .maxAge(3600); // 预检请求（OPTIONS）缓存时间（1小时）
    }
}