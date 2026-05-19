package com.funfun.schedule.controller.financialplan;

import java.util.UUID;

/**
 * 轻量 traceId 工具，用于在错误信息中携带请求标识，便于日志关联与定位。
 *
 * <p>取 UUID 前 16 位 hex，足以在单实例日志中唯一识别一次请求。
 */
public final class WebTraceContext {

    private WebTraceContext() {
    }

    /**
     * 生成一个新的 traceId。
     */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
