package com.funfun.schedule.service;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.config.WeChatConfig;
import com.funfun.schedule.util.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * 微信小程序码生成服务。
 *
 * 核心：用分享 token 作为 scene、{@code pages/task/share} 作为 page，调用微信
 * getUnlimitedQRCode（getwxacodeunlimit）生成「无限制小程序码」。用户在微信里长按识别该码
 * 即可打开小程序对应页面，query 中会带上 scene=token。
 *
 * dev 环境（占位 appid）或微信调用失败时，降级为 JDK 本地生成的占位二维码图，
 * 保证前端 canvas 仍能渲染、便于联调；不引入额外二维码依赖。
 *
 * 文档：https://developers.weixin.qq.com/miniprogram/dev/server/API/qrcode-link/qr-code/api_getunlimitedqrcode.html
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeChatQrcodeService {

    private final WeChatConfig weChatConfig;

    // access_token 进程内缓存（微信限制：同 appid 全局唯一、有效期 7200s）。
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAt; // epoch millis

    /**
     * 生成无限制小程序码。
     *
     * @param scene 场景值（≤32 个可见字符），这里传分享 token
     * @param page  打开的小程序页面路径（不含前导斜杠、不带 query），如 pages/task/share
     * @return PNG 图片字节
     */
    public byte[] getUnlimitedQrCode(String scene, String page) {
        if (!StringUtils.hasText(scene)) {
            throw new IllegalArgumentException("scene 不能为空");
        }
        if (isPlaceholderConfig()) {
            log.info("微信小程序码：当前为占位/未配置 appid，返回 dev 占位二维码。scene={}, page={}", scene, page);
            return buildPlaceholderQr(scene);
        }
        try {
            String accessToken = getAccessToken();
            String url = new URIBuilder(weChatConfig.getApiBaseUrl() + "/wxa/getwxacodeunlimit")
                    .setParameter("access_token", accessToken)
                    .build()
                    .toString();

            JSONObject body = new JSONObject();
            body.put("scene", scene);
            if (StringUtils.hasText(page)) {
                body.put("page", page);
            }
            body.put("check_path", weChatConfig.isQrcodeCheckPath());
            body.put("env_version", weChatConfig.getQrcodeEnvVersion());
            body.put("width", 430);
            body.put("auto_color", false);

            byte[] resp = HttpUtil.postForBytes(url, body.toJSONString());
            // 失败时微信返回 JSON（以 '{' 开头），成功时返回图片二进制。
            if (looksLikeJson(resp)) {
                String errText = new String(resp, StandardCharsets.UTF_8);
                log.warn("微信生成小程序码失败，降级为占位码。响应：{}", errText);
                return buildPlaceholderQr(scene);
            }
            return resp;
        } catch (Exception e) {
            log.warn("微信生成小程序码异常，降级为占位码：{}", e.getMessage());
            return buildPlaceholderQr(scene);
        }
    }

    /** 获取 access_token（带进程内缓存，提前 5 分钟过期刷新）。 */
    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedAccessToken) && now < accessTokenExpireAt) {
            return cachedAccessToken;
        }
        String url;
        try {
            url = new URIBuilder(weChatConfig.getApiBaseUrl() + "/cgi-bin/token")
                    .setParameter("grant_type", "client_credential")
                    .setParameter("appid", weChatConfig.getAppid())
                    .setParameter("secret", weChatConfig.getSecret())
                    .build()
                    .toString();
        } catch (Exception e) {
            throw new IllegalStateException("构建 access_token 请求 URL 失败", e);
        }
        String response = HttpUtil.get(url);
        JSONObject json = JSONObject.parseObject(response);
        String accessToken = json.getString("access_token");
        Integer expiresIn = json.getInteger("expires_in");
        if (!StringUtils.hasText(accessToken)) {
            throw new RuntimeException("获取微信 access_token 失败：" + response);
        }
        cachedAccessToken = accessToken;
        // 提前 5 分钟过期，避免边界失效
        accessTokenExpireAt = now + ((expiresIn == null ? 7200 : expiresIn) - 300) * 1000L;
        return accessToken;
    }

    private boolean isPlaceholderConfig() {
        String appid = weChatConfig.getAppid();
        return !StringUtils.hasText(appid)
                || appid.equals(weChatConfig.getPlaceholderAppid())
                || appid.startsWith("your-");
    }

    private boolean looksLikeJson(byte[] data) {
        if (data == null || data.length == 0) {
            return true;
        }
        // 跳过可能的 BOM / 空白
        for (byte b : data) {
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
                continue;
            }
            return b == '{';
        }
        return false;
    }

    /**
     * 本地生成一张「占位二维码」PNG：仅用于 dev 联调时让 canvas 有图可画，并非可扫码的真实小程序码。
     * 用 JDK 自带 BufferedImage/ImageIO，无第三方依赖；headless 环境可用。
     */
    private byte[] buildPlaceholderQr(String scene) {
        int size = 430;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);

            // 用 scene 做种子生成稳定的伪随机点阵，外观像二维码
            int margin = 40;
            int grid = 21;
            int cell = (size - margin * 2) / grid;
            Random rnd = new Random(scene.hashCode());
            g.setColor(new Color(0x1f2937));
            for (int r = 0; r < grid; r++) {
                for (int c = 0; c < grid; c++) {
                    if (rnd.nextBoolean()) {
                        g.fillRect(margin + c * cell, margin + r * cell, cell, cell);
                    }
                }
            }
            // 三个定位角（finder pattern）
            drawFinder(g, margin, margin, cell);
            drawFinder(g, margin + (grid - 7) * cell, margin, cell);
            drawFinder(g, margin, margin + (grid - 7) * cell, cell);

            // 中央文案块
            int boxW = size - margin * 2;
            int boxH = 64;
            int boxY = (size - boxH) / 2;
            g.setColor(Color.WHITE);
            g.fillRect(margin, boxY, boxW, boxH);
            g.setColor(new Color(0x2196f3));
            g.setStroke(new BasicStroke(2f));
            g.drawRect(margin, boxY, boxW, boxH);
            g.setColor(new Color(0x2196f3));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            String text = "DEV 占位码";
            int tw = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (size - tw) / 2, boxY + 40);

            g.setColor(new Color(0x94a3b8));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String hint = "配置真实 appid 后生成可扫码小程序码";
            int hw = g.getFontMetrics().stringWidth(hint);
            g.drawString(hint, (size - hw) / 2, boxY + boxH + 30);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成占位二维码失败", e);
        }
    }

    private void drawFinder(Graphics2D g, int x, int y, int cell) {
        int s = cell * 7;
        g.setColor(new Color(0x1f2937));
        g.fillRect(x, y, s, s);
        g.setColor(Color.WHITE);
        g.fillRect(x + cell, y + cell, s - cell * 2, s - cell * 2);
        g.setColor(new Color(0x1f2937));
        g.fillRect(x + cell * 2, y + cell * 2, s - cell * 4, s - cell * 4);
    }
}
