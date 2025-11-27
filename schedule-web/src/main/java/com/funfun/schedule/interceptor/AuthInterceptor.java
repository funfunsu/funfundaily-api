package com.funfun.schedule.interceptor;
// src/main/java/com/example/interceptor/LoginInterceptor.java

import com.funfun.schedule.anno.NoAuth;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.util.LoginCheckUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private LoginCheckUtil loginCheckUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        // 如果方法上有 @NoAuth 注解，放行
        if (method.getMethodAnnotation(NoAuth.class) != null) {
            return true;
        }
        // 校验 token 是否存在（假设 token 存在 Redis 中，key: login:token:{token}）
        Long userId = loginCheckUtil.checkLoginAndGetUserId(request);
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

}