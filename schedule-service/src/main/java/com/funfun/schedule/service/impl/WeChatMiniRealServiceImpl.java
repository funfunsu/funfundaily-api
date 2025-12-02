package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.config.WeChatConfig;
import com.funfun.schedule.service.SessionKeyService;
import com.funfun.schedule.service.WeChatMiniService;
import com.funfun.schedule.util.HttpUtil; // 引入我们封装的工具类
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 微信小程序服务真实实现（生产环境用，调用真实微信接口）
 */
@Service
@Profile("prod") // 仅在 prod 环境激活
@RequiredArgsConstructor
@Slf4j
public class WeChatMiniRealServiceImpl implements WeChatMiniService {

    private final WeChatConfig weChatConfig;
    private final SessionKeyService sessionKeyService;

    @Override
    public String jscode2session(String code) {
        log.info(JSONObject.toJSONString(weChatConfig));
        // 安全构建 URL（自动处理参数编码）
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
            log.error("构建微信 code2session 请求 URL 失败", e);
            throw new IllegalArgumentException("无效的微信配置或 code", e);
        }

        // 使用封装好的 HttpUtil 发起 GET 请求（带重试、日志、超时）
        String response = HttpUtil.get(url);

        log.info("微信 code2session 真实响应：{}", response);
        JSONObject json = JSONObject.parseObject(response);

        if (json.containsKey("errcode") && json.getInteger("errcode") != 0) {
            String errmsg = json.getString("errmsg");
            log.warn("真实微信接口调用失败：errcode={}, errmsg={}",
                    json.getInteger("errcode"), errmsg);
            throw new RuntimeException("真实微信接口调用失败：" + errmsg);
        }

        String openId = json.getString("openid");
        String sessionKey = json.getString("session_key");

        if (openId == null || sessionKey == null) {
            throw new IllegalStateException("微信响应缺少 openid 或 session_key");
        }

        // 存储 session_key 到数据库（支持集群共享）
        sessionKeyService.saveOrUpdate(weChatConfig.getAppid(), openId, sessionKey);

        return openId;
    }
}