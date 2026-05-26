package com.funfun.schedule.mcp;

import com.funfun.schedule.dto.TodoTaskDTO;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.service.TodoTaskService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进程内 MCP 工具：把待办任务能力暴露给 MCP 客户端。
 *
 * <p>由 Spring AI 的 MCP server（WebMVC/Streamable HTTP 传输）托管，运行在后端进程内（不独立部署）。
 * 鉴权与数据隔离：MCP 端点 /mcp 经 {@code McpAuthFilter} 校验 Bearer Token，
 * 再由 {@link McpToolsConfig} 配置的 contextExtractor 把 groupId / userId 快照进
 * {@link McpTransportContext}；工具经 {@code exchange.transportContext()} 读取（跨线程稳定）。
 */
@Component
public class TaskMcpTools {

    private final TodoTaskService todoTaskService;

    public TaskMcpTools(TodoTaskService todoTaskService) {
        this.todoTaskService = todoTaskService;
    }

    @Tool(name = "get_todo_task_list",
            description = "获取当前令牌所属群组的待办任务列表（按创建时间升序）。可选按 userId、parentId 过滤；不传 userId 时默认令牌绑定的成员。返回每个任务的标题与内容。")
    public List<TodoTaskDTO> getToDoTaskList(
            @ToolParam(required = false, description = "按归属成员过滤（不传默认令牌绑定成员）") Long userId,
            @ToolParam(required = false, description = "按父任务 ID 过滤（可选）") Long parentId,
            ToolContext toolContext) {
        return todoTaskService.getTodoTaskList(requireGroupId(toolContext), resolveUserId(toolContext, userId), parentId);
    }

    @Tool(name = "get_next_todo_task",
            description = "按创建时间排序，返回下一个（最早创建且当前周期未完成的）待办任务。可选按 userId、parentId 过滤；不传 userId 时默认令牌绑定的成员；无未完成任务时返回 null。")
    public TodoTaskDTO getNextTodoTask(
            @ToolParam(required = false, description = "按归属成员过滤（不传默认令牌绑定成员）") Long userId,
            @ToolParam(required = false, description = "按父任务 ID 过滤（可选）") Long parentId,
            ToolContext toolContext) {
        return todoTaskService.getNextTodoTask(requireGroupId(toolContext), resolveUserId(toolContext, userId), parentId);
    }

    @Tool(name = "check_in_task",
            description = "对指定任务执行一次打卡 / 完成。taskId 必填；userId 可选（不传默认令牌绑定成员）；taskTime 可选（ISO 时间如 2026-05-23T09:00:00，默认当天）。返回打卡记录 ID。")
    public Long checkInTask(
            @ToolParam(required = true, description = "任务 ID") Long taskId,
            @ToolParam(required = false, description = "打卡成员 ID（不传默认令牌绑定成员）") Long userId,
            @ToolParam(required = false, description = "打卡所属时间 ISO 字符串，如 2026-05-23T09:00:00") String taskTime,
            ToolContext toolContext) {
        LocalDateTime when = (taskTime == null || taskTime.isBlank()) ? null : LocalDateTime.parse(taskTime.trim());
        return todoTaskService.checkInTask(requireGroupId(toolContext), taskId, resolveUserId(toolContext, userId), when);
    }

    /** 取本次 MCP 调用的传输上下文（由 contextExtractor 在 HTTP 线程写入）。 */
    private McpTransportContext transportContext(ToolContext toolContext) {
        return McpToolUtils.getMcpExchange(toolContext)
                .map(McpSyncServerExchange::transportContext)
                .orElse(McpTransportContext.EMPTY);
    }

    /** 取本次请求令牌绑定的 groupId；缺失说明未鉴权。 */
    private Long requireGroupId(ToolContext toolContext) {
        Object groupId = transportContext(toolContext).get(McpToolsConfig.GROUP_ID_KEY);
        if (!(groupId instanceof Long)) {
            CommonException.LOGIN_INVALID.throwsError("MCP 请求未携带有效访问令牌");
        }
        return (Long) groupId;
    }

    /** 未显式传 userId 时回退到令牌绑定的成员（group_member.user_id）。 */
    private Long resolveUserId(ToolContext toolContext, Long requestUserId) {
        if (requestUserId != null) {
            return requestUserId;
        }
        Object userId = transportContext(toolContext).get(McpToolsConfig.USER_ID_KEY);
        return (userId instanceof Long) ? (Long) userId : null;
    }
}
