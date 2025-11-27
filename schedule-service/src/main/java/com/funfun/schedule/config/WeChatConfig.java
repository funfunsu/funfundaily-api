package com.funfun.schedule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置绑定
 */
@Component
@ConfigurationProperties(prefix = "wechat.mini")
@Data
public class WeChatConfig {
    private String appid; // 小程序appid
    private String secret; // 小程序secret
    private String jscode2sessionUrl; // code2session接口地址
}