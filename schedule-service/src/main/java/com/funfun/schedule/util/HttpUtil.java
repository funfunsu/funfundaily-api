package com.funfun.schedule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
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

    /**
     * 发送 POST 请求（JSON 请求体），返回原始响应字节。
     * 微信 getwxacodeunlimit 成功时返回二进制图片、失败时返回 JSON，故统一按 byte[] 取回，
     * 由调用方根据响应内容（是否以 '{' 开头）区分图片与错误。
     *
     * @param url      完整请求地址（含 access_token 等查询参数）
     * @param jsonBody JSON 字符串请求体
     * @return 响应体字节（2xx）
     * @throws RuntimeException 网络异常、超时、非 2xx 响应等
     */
    public static byte[] postForBytes(String url, String jsonBody) {
        long start = System.currentTimeMillis();
        log.debug("HTTP POST 请求: {} | body: {}", url, jsonBody);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept", "*/*");
        httpPost.setHeader("User-Agent", "FunFun-Schedule/1.0");
        httpPost.setEntity(new StringEntity(jsonBody == null ? "" : jsonBody,
                ContentType.create("application/json", StandardCharsets.UTF_8)));

        try (CloseableHttpResponse response = httpClient.execute(httpPost, HttpClientContext.create())) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            byte[] body = entity != null ? EntityUtils.toByteArray(entity) : new byte[0];

            long cost = System.currentTimeMillis() - start;
            log.info("HTTP POST 响应 [{} ms] | URL: {} | Status: {} | Body length: {}",
                    cost, url, statusCode, body.length);

            if (statusCode >= 200 && statusCode < 300) {
                return body;
            }
            throw new RuntimeException("HTTP POST 请求失败，状态码: " + statusCode);
        } catch (IOException e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("HTTP POST 异常 | URL: {} | 耗时: {} ms | 错误: {}", url, cost, e.getMessage());
            throw new RuntimeException("HTTP POST 请求失败: " + e.getMessage(), e);
        }
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