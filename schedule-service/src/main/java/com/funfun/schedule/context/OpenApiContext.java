package com.funfun.schedule.context;

/**
 * 开放接口（OpenAPI / MCP）请求上下文。
 *
 * <p>在 Bearer Token 鉴权通过后，由拦截器写入当前请求绑定的 groupId 与令牌名称，
 * 供下游 Service 做基于 groupId 的数据隔离。每个请求结束后必须 {@link #clear()}。
 */
public class OpenApiContext {

    private static final ThreadLocal<Long> GROUP_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_NAME = new ThreadLocal<>();

    public static void setGroupId(Long groupId) {
        GROUP_ID.set(groupId);
    }

    public static Long getGroupId() {
        return GROUP_ID.get();
    }

    public static void setTokenName(String tokenName) {
        TOKEN_NAME.set(tokenName);
    }

    public static String getTokenName() {
        return TOKEN_NAME.get();
    }

    public static void clear() {
        GROUP_ID.remove();
        TOKEN_NAME.remove();
    }
}
