package com.funfun.schedule.context;

/**
 * 开放接口（OpenAPI / MCP）请求上下文。
 *
 * <p>在 Bearer Token 鉴权通过后，由拦截器/过滤器写入当前请求绑定的 groupId 与 userId
 * （令牌绑定到 group_member 行，因此可同时确定 groupId 与 userId）。
 * 下游 Service 据此做基于 groupId 的数据隔离，userId 可用于审计/默认归属。
 * 每个请求结束后必须 {@link #clear()}。
 */
public class OpenApiContext {

    private static final ThreadLocal<Long> GROUP_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setGroupId(Long groupId) {
        GROUP_ID.set(groupId);
    }

    public static Long getGroupId() {
        return GROUP_ID.get();
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        GROUP_ID.remove();
        USER_ID.remove();
    }
}
