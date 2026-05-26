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
            String body = new String(content, StandardCharsets.UTF_8);
            msg.append(",Request Body:").append(body);
        }
        return msg.toString();
    }

    private String getResponseStr(ContentCachingResponseWrapper response) {
        StringBuilder msg = new StringBuilder();
        // 打印 Response Body
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            msg.append(",Response Body:").append(body);
        }
        return msg.toString();
    }
}