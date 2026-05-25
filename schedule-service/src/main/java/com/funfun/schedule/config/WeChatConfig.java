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

    /** 微信开放接口基础域名，默认正式环境；可在 yml 覆盖 */
    private String apiBaseUrl = "https://api.weixin.qq.com";
    /** 小程序码 page 对应的小程序版本：release / trial / develop（默认 release） */
    private String qrcodeEnvVersion = "release";
    /** 生成小程序码时是否校验 page 是否已发布（dev/未发布页面需设 false） */
    private boolean qrcodeCheckPath = false;
    /** dev 占位 appid（等于该值或为空时跳过真实微信调用，返回占位二维码） */
    private String placeholderAppid = "your-wechat-dev-appid";
}