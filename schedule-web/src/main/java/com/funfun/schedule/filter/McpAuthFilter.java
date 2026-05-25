package com.funfun.schedule.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.context.OpenApiContext;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.OpenApiPrincipal;
import com.funfun.schedule.service.OpenApiTokenService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 进程内 MCP 服务的 Bearer Token 鉴权过滤器。
 *
 * <p>为什么用 Filter 而不是 HandlerInterceptor：Spring AI MCP 的端点 {@code /mcp}
 * 是基于 {@code RouterFunction} 的函数式端点，{@code WebMvcConfigurer} 注册的拦截器对其不生效；
 * 而 Servlet Filter 对所有请求都生效。MCP（SYNC）在该 POST 请求线程上同步执行工具，
 * 因此在此线程的 ThreadLocal（{@link OpenApiContext}）能被 @Tool 方法读到。
 *
 * <p>Streamable HTTP 传输只有单端点 {@code /mcp}（POST 承载 initialize/tools/call，GET 为可选
 * SSE 监听流）。这里对 {@code /mcp} 的所有请求统一校验 Bearer Token —— 客户端（type:http）
 * 会在每个请求都带上 Authorization 头。
 */
@Component
@Order(0) // 早于 LoggingFilter(@Order 1)
public class McpAuthFilter implements Filter {

    private static final String MCP_ENDPOINT = "/mcp";

    private final OpenApiTokenService tokenService;
    private final ObjectMapper objectMapper;

    public McpAuthFilter(OpenApiTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (!MCP_ENDPOINT.equals(req.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(resp, "未携带访问令牌");
            return;
        }
        OpenApiPrincipal principal = tokenService.resolvePrincipal(header.substring(7).trim());
        if (principal == null) {
            writeUnauthorized(resp, "访问令牌无效或已禁用");
            return;
        }

        try {
            OpenApiContext.setGroupId(principal.groupId());
            OpenApiContext.setUserId(principal.userId());
            chain.doFilter(request, response);
        } finally {
            OpenApiContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CommonResponse<Void> body = CommonResponse.fail(CommonException.LOGIN_INVALID.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
