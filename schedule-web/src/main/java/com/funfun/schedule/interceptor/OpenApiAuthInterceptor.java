package com.funfun.schedule.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.config.OpenApiTokenConfig;
import com.funfun.schedule.context.OpenApiContext;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 开放接口（OpenAPI / MCP）Bearer Token 鉴权拦截器。
 *
 * <p>仅作用于 {@code /openapi/**}（与小程序的 JWT 鉴权链路 {@code /api/**} 互不影响）。
 * 校验 {@code Authorization: Bearer <token>}，把 token 解析为绑定的 groupId 写入
 * {@link OpenApiContext}，下游据此做数据隔离。
 */
@Component
@Slf4j
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    private final OpenApiTokenConfig tokenConfig;
    private final ObjectMapper objectMapper;

    public OpenApiAuthInterceptor(OpenApiTokenConfig tokenConfig, ObjectMapper objectMapper) {
        this.tokenConfig = tokenConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行非 Controller 处理器（含 CORS 预检 PreFlightHandler）。
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(response, "未携带访问令牌");
        }
        String token = header.substring(7).trim();

        OpenApiTokenConfig.TokenEntry entry = tokenConfig.resolve(token);
        if (entry == null || entry.getGroupId() == null) {
            return unauthorized(response, "访问令牌无效或已禁用");
        }

        OpenApiContext.setGroupId(entry.getGroupId());
        OpenApiContext.setTokenName(entry.getName());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        OpenApiContext.clear();
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CommonResponse<Void> body =
                CommonResponse.fail(CommonException.LOGIN_INVALID.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
