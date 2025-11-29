package com.funfun.schedule.utils;

import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: Sue Su
 * @Date: 2025-11-20 15:25
 * @Description:
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class LoginCheckUtil {
    private final JwtUtil jwtUtil;

    public Long checkLoginAndGetUserId(HttpServletRequest request) {
        // 2. 从请求头获取Token（前端需在Authorization头携带Token，格式：Bearer {token}）
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty() || !token.startsWith("Bearer ")) {
            CommonException.LOGIN_INVALID.throwsError("未携带登录Token");
        }
        token = token.substring(7);

        // 3. 验证Token有效性
        if (!jwtUtil.validateToken(token)) {
            CommonException.LOGIN_INVALID.throwsError("Token无效或已过期");
        }
        // 4. 解析Token获取userId（openid）
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null || userId.isEmpty()) {
            CommonException.LOGIN_INVALID.throwsError("Token解析失败，未获取到userId");
        }
        return Long.valueOf(userId);
    }
}
