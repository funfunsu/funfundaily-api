package com.funfun.schedule.filter; // 请根据实际结构调整包名

import com.alibaba.fastjson2.JSON;
import com.funfun.schedule.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Order(1) // 设置过滤器顺序，确保它尽早执行
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    /** 文本类 body 最大打印长度，超出部分截断（避免大 body 刷屏） */
    private static final int MAX_BODY_LENGTH = 2000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // SSE / 流式端点（如进程内 MCP 的 /mcp/sse）不能用 ContentCachingResponseWrapper 包装：
        // 它会缓冲响应体、直到请求结束才 copyBodyToResponse，导致 SSE 事件无法实时下发。
        // 因此对 /mcp/** 直接放行（不缓存、不记录响应体），保证流式推送正常。
        if (httpRequest.getRequestURI().startsWith("/mcp/")) {
            chain.doFilter(request, response);
            return;
        }

        // 使用 ContentCachingWrapper 包装请求和响应，以便缓存内容供后续读取
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        try {
            // 继续执行过滤器链
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // 记录日志
            String requestStr = getRequestStr(wrappedRequest);
            String responseStr = getResponseStr(wrappedResponse);

            // 重要：必须复制响应体内容回原始响应，否则客户端收不到响应数据
            wrappedResponse.copyBodyToResponse();
            logger.info("userId = {},URI={},method={},status:{},cost:{}ms,{},{}",UserContext.getUserId(),wrappedRequest.getRequestURI(),wrappedRequest.getMethod(),wrappedResponse.getStatus(),duration, requestStr, responseStr);
            UserContext.clear();
        }
    }

    private String getRequestStr(ContentCachingRequestWrapper request) {
        StringBuilder msg = new StringBuilder();
        // 打印 Parameters (Query String)
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            msg.append("Parameters:").append(JSON.toJSONString(params));
        }
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            msg.append(",Request Body:").append(formatBody(request.getContentType(), content));
        }
        return msg.toString();
    }

    private String getResponseStr(ContentCachingResponseWrapper response) {
        StringBuilder msg = new StringBuilder();
        // 打印 Response Body
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            msg.append(",Response Body:").append(formatBody(response.getContentType(), content));
        }
        return msg.toString();
    }

    /**
     * 格式化 body 用于日志：
     * 1. 图片/二进制类（image、multipart、audio、video、octet-stream、pdf 等）整体打印没有参考价值，
     *    只记录 contentType 与字节数，不打印内容；
     * 2. 文本类 body 超过 {@link #MAX_BODY_LENGTH} 时截断，附带原始字节数。
     */
    private String formatBody(String contentType, byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        if (isBinaryContent(contentType)) {
            return "[binary body omitted, contentType=" + contentType + ", size=" + content.length + " bytes]";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        if (body.length() > MAX_BODY_LENGTH) {
            return body.substring(0, MAX_BODY_LENGTH)
                    + "...(truncated, total " + content.length + " bytes)";
        }
        return body;
    }

    /** 判断是否为不适合明文打印的二进制/媒体类型 */
    private boolean isBinaryContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        return ct.startsWith("image/")
                || ct.startsWith("multipart/")
                || ct.startsWith("audio/")
                || ct.startsWith("video/")
                || ct.contains("octet-stream")
                || ct.startsWith("application/pdf");
    }
}