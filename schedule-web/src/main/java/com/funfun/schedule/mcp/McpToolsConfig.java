package com.funfun.schedule.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.context.OpenApiContext;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 把 {@link TaskMcpTools} 上的 @Tool 方法注册给 Spring AI MCP server，
 * 并自定义 Streamable HTTP 传输的 {@link McpTransportContextExtractor}，
 * 以便把鉴权身份（groupId / userId）安全地带到（异步）工具执行线程。
 */
@Configuration
public class McpToolsConfig {

    /** {@link McpTransportContext} 中保存 groupId 的键。 */
    public static final String GROUP_ID_KEY = "funfun.groupId";
    /** {@link McpTransportContext} 中保存 userId 的键。 */
    public static final String USER_ID_KEY = "funfun.userId";

    @Bean
    public ToolCallbackProvider taskToolCallbackProvider(TaskMcpTools taskMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(taskMcpTools)
                .build();
    }

    /**
     * 覆盖 Spring AI 自动配置的 Streamable HTTP 传输（其默认不带 contextExtractor），
     * 加上从 HTTP 请求线程提取鉴权身份的 extractor。
     *
     * <p>为什么需要：Streamable HTTP 下工具在异步 / reactor 线程执行，
     * {@code McpAuthFilter} 写在 HTTP 线程 ThreadLocal（{@link OpenApiContext}）里的 groupId
     * 不一定能传播过去。这里在 HTTP 线程（extractor 运行于 RouterFunction 处理中、
     * 仍在 filter 链内、ThreadLocal 已被 McpAuthFilter 写入）把身份快照进
     * {@link McpTransportContext}，由 MCP SDK 随会话带到工具执行处，工具再经
     * {@code exchange.transportContext()} 读取，从而保证数据隔离稳定生效。
     *
     * <p>自动配置的同名 Bean 标了 {@code @ConditionalOnMissingBean}，此处定义后它会退避。
     */
    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            ObjectMapper objectMapper, McpServerStreamableHttpProperties properties) {
        McpTransportContextExtractor<ServerRequest> extractor = request -> {
            Long groupId = OpenApiContext.getGroupId();
            if (groupId == null) {
                return McpTransportContext.EMPTY;
            }
            Map<String, Object> ctx = new HashMap<>(4);
            ctx.put(GROUP_ID_KEY, groupId);
            Long userId = OpenApiContext.getUserId();
            if (userId != null) {
                ctx.put(USER_ID_KEY, userId);
            }
            return McpTransportContext.create(ctx);
        };

        WebMvcStreamableServerTransportProvider.Builder builder =
                WebMvcStreamableServerTransportProvider.builder()
                        .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                        .mcpEndpoint(properties.getMcpEndpoint())
                        .disallowDelete(properties.isDisallowDelete())
                        .contextExtractor(extractor);
        if (properties.getKeepAliveInterval() != null) {
            builder.keepAliveInterval(properties.getKeepAliveInterval());
        }
        return builder.build();
    }
}
