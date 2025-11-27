package com.funfun.schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

/**
 * 自定义跨域配置（复杂场景用）
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 执行顺序最高，确保早于其他 Filter（如鉴权、日志）
    public CorsWebFilter corsWebFilter() {
        // 1. 配置跨域规则
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 允许的跨域源（动态配置示例：可从配置中心/数据库读取）
        List<String> allowedOrigins = Arrays.asList(
                "http://localhost:8080",
                "https://xxx-frontend.com",
                "https://test-xxx-frontend.com"
        );
        corsConfig.setAllowedOrigins(allowedOrigins);

        // 允许的请求方法
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头（包含自定义头）
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Trace-Id", "X-Requested-With"));
        // 简化配置：允许所有请求头（生产环境慎用）
        // corsConfig.setAllowedHeaders(Arrays.asList("*"));

        // 允许前端获取的响应头（必须包含自定义响应头）
        corsConfig.setExposedHeaders(Arrays.asList("X-Cost-Time-MS", "X-Trace-Id"));

        // 允许跨域携带 Cookie/Token
        corsConfig.setAllowCredentials(true);

        // 预检请求缓存时间（1 小时）
        corsConfig.setMaxAge(3600L);

        // 2. 配置跨域规则生效的路径（匹配所有路径）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", corsConfig); // 所有路径生效
        // 仅对 api 路径生效：source.registerCorsConfiguration("/api/**", corsConfig);

        // 3. 创建并返回 CorsWebFilter
        return new CorsWebFilter(source);
    }
}