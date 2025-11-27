package com.funfun.schedule.service.impl;

import com.funfun.schedule.service.SessionKeyService;
import com.funfun.schedule.service.WeChatMiniService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 微信小程序服务 Mock 实现（本地开发用，不调用真实微信接口）
 */
@Service
@Profile("dev") // 关键：仅当激活 dev 环境时，该 Bean 才会被 Spring 加载
@RequiredArgsConstructor
@Slf4j
public class WeChatMiniMockServiceImpl implements WeChatMiniService {

    private final SessionKeyService sessionKeyService;

    @Override
    public String jscode2session(String code) {
        // 本地开发：直接返回 mock 数据（无需调用微信接口）
        log.info("=== 本地开发模式：Mock 微信 jscode2session 接口 ===");
        log.info("传入的 code：{}", code);

        // 1. 生成 mock openId（可根据 code 动态生成，模拟多用户）
        String mockOpenId = "mock-openid-" + code; // 比如 code=123 → openId=mock-openid-123
        // 2. 生成 mock sessionKey（必须 32 位，否则解密会失败）
        String mockSessionKey = "mock-sessionkey-32bytes-12345678";

        // 3. 存储 mock sessionKey 到 MySQL（与真实环境逻辑一致，确保后续解密正常）
        sessionKeyService.saveOrUpdate("testAppId",mockOpenId, mockSessionKey);

        log.info("Mock 数据返回：openId={}, sessionKey={}", mockOpenId, mockSessionKey);
        log.info("=== Mock 接口调用结束 ===");

        // 返回 mock openId（与真实环境返回格式一致）
        return mockOpenId;
    }
}