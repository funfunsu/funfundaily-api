package com.funfun.schedule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关认证配置绑定（替代 @Value 占位符）
 */
@Component
@ConfigurationProperties(prefix = "app.cors") // 对应 yml 中的 gateway.auth 层级
@Data
public class CorsConfig {
    private List<String> allowedOriginPatterns; // 自动绑定 gateway.auth.whitelist
}