package com.funfun.schedule.mcp;

import com.funfun.schedule.context.OpenApiContext;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Hooks;
import org.springframework.context.annotation.Configuration;

/**
 * 让 {@link OpenApiContext} 的 groupId / userId 跨线程传播。
 *
 * <p>进程内 MCP（Spring AI，SYNC）底层用 Reactor，工具最终在 reactor 线程上执行，
 * 而 {@code McpAuthFilter} 是在 HTTP 请求线程上把 groupId / userId 写入 ThreadLocal。
 * 通过注册 {@code ThreadLocalAccessor} + 开启 Reactor 自动上下文传播，
 * 订阅时（HTTP 线程）捕获快照，并在工具执行线程上恢复，从而保证数据隔离生效。
 */
@Configuration
public class ReactorContextPropagationConfig {

    /** Reactor Context 中保存 groupId 的键。 */
    public static final String GROUP_ID_KEY = "funfun.openapi.groupId";
    /** Reactor Context 中保存 userId 的键。 */
    public static final String USER_ID_KEY = "funfun.openapi.userId";

    @PostConstruct
    public void enableContextPropagation() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                GROUP_ID_KEY,
                OpenApiContext::getGroupId,
                OpenApiContext::setGroupId,
                () -> {/* 由 USER_ID 的 accessor 统一 clear，避免重复清理 */});
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                USER_ID_KEY,
                OpenApiContext::getUserId,
                OpenApiContext::setUserId,
                OpenApiContext::clear);
        Hooks.enableAutomaticContextPropagation();
    }
}
