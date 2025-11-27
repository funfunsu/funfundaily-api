package com.funfun.schedule.util;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.config.WeChatConfig;
import com.funfun.schedule.service.SessionKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Component;

/**
 * 微信小程序接口调用工具（使用封装后的 HttpUtil）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeChatMiniUtil {

    private final WeChatConfig weChatConfig;
    private final SessionKeyService sessionKeyService;

    public String jscode2session(String code) {
        // 安全构建 URL（自动编码）
        String url;
        try {
            url = new URIBuilder(weChatConfig.getJscode2sessionUrl())
                    .setParameter("appid", weChatConfig.getAppid())
                    .setParameter("secret", weChatConfig.getSecret())
                    .setParameter("js_code", code)
                    .setParameter("grant_type", "authorization_code")
                    .build()
                    .toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("构建微信请求 URL 失败", e);
        }

        // 使用通用 HTTP 工具发起请求（带重试和日志）
        String response = HttpUtil.get(url);

        log.info("微信 code2session 响应: {}", response);
        JSONObject json = JSONObject.parseObject(response);

        if (json.containsKey("errcode") && json.getInteger("errcode") != 0) {
            String errmsg = json.getString("errmsg");
            log.warn("微信登录失败: errcode={}, errmsg={}", json.getInteger("errcode"), errmsg);
            throw new RuntimeException("调用微信登录接口失败: " + errmsg);
        }

        String openId = json.getString("openid");
        String sessionKey = json.getString("session_key");

        if (openId == null || sessionKey == null) {
            throw new IllegalStateException("微信响应缺少 openid 或 session_key");
        }

        sessionKeyService.saveOrUpdate(weChatConfig.getAppid(), openId, sessionKey);
        return openId;
    }
}