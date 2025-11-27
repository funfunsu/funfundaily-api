package com.funfun.schedule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 通用 HTTP 工具类（基于 Apache HttpClient 4.5+）
 * 支持重试、超时、日志、自动资源释放
 */
@Slf4j
public class HttpUtil {

    private static final CloseableHttpClient httpClient;

    // 配置参数（可根据需要提取为配置类）
    private static final int CONNECT_TIMEOUT = 5000;     // 连接超时 5s
    private static final int SOCKET_TIMEOUT = 10000;     // 读取超时 10s
    private static final int RETRY_TIMES = 2;            // 重试 2 次（共尝试 3 次）
    private static final long RETRY_INTERVAL_MS = 500;   // 重试间隔 500ms

    static {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .build();

        // 使用标准重试处理器（对 IOException 重试，幂等方法如 GET/HEAD）
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(new StandardHttpRequestRetryHandler(RETRY_TIMES, true))
                .build();
    }

    /**
     * 发送 GET 请求，返回响应体字符串
     *
     * @param url 请求地址（必须是完整 URL）
     * @return 响应体（UTF-8 解码）
     * @throws RuntimeException 网络异常、超时、非 2xx 响应等
     */
    public static String get(String url) {
        long start = System.currentTimeMillis();
        log.debug("HTTP GET 请求: {}", url);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json, text/plain, */*");
        httpGet.setHeader("User-Agent", "FunFun-Schedule/1.0");

        for (int attempt = 0; attempt <= RETRY_TIMES; attempt++) {
            try (CloseableHttpResponse response = httpClient.execute(httpGet, HttpClientContext.create())) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";

                long cost = System.currentTimeMillis() - start;
                log.info("HTTP GET 响应 [{} ms] | URL: {} | Status: {} | Body length: {}",
                        cost, url, statusCode, body.length());

                if (statusCode >= 200 && statusCode < 300) {
                    return body;
                } else {
                    log.warn("HTTP GET 非成功响应 [尝试 {}/{}] | Status: {} | Body: {}",
                            attempt + 1, RETRY_TIMES + 1, statusCode, body);
                    if (attempt < RETRY_TIMES) {
                        sleep(RETRY_INTERVAL_MS);
                        continue; // 重试
                    }
                    throw new RuntimeException("HTTP 请求失败，状态码: " + statusCode + ", 响应: " + body);
                }

            } catch (IOException e) {
                long cost = System.currentTimeMillis() - start;
                log.warn("HTTP GET 异常 [尝试 {}/{}] | URL: {} | 耗时: {} ms | 错误: {}",
                        attempt + 1, RETRY_TIMES + 1, url, cost, e.getMessage());

                if (attempt < RETRY_TIMES) {
                    sleep(RETRY_INTERVAL_MS);
                    continue; // 重试
                }
                throw new RuntimeException("HTTP 请求最终失败: " + e.getMessage(), e);
            }
        }

        // 理论上不会走到这里
        throw new IllegalStateException("Unexpected execution path in HttpUtil.get()");
    }

    // 辅助方法：安全睡眠（不抛中断异常）
    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("重试等待被中断", e);
        }
    }

    // 可选：提供关闭方法（一般不需要，JVM 退出时自动关闭）
    public static void close() throws IOException {
        httpClient.close();
    }
}