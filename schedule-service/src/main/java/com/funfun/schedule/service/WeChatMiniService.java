package com.funfun.schedule.service;

/**
 * 微信小程序服务接口（统一方法签名，适配多环境）
 */
public interface WeChatMiniService {
    /**
     * 调用 code2session 获取 openId 和 sessionKey
     * @param code 小程序端传入的 code
     * @return Mono<String> 微信 openId
     */
    String jscode2session(String code);
}