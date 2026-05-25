package com.funfun.schedule.controller.openapi;

import com.funfun.schedule.context.OpenApiContext;
import com.funfun.schedule.dto.TodoTaskDTO;
import com.funfun.schedule.dto.openapi.OpenCheckinRequest;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.TodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放接口（OpenAPI / MCP）任务能力。
 *
 * <p>路径前缀 {@code /openapi/**}，由 {@code OpenApiAuthInterceptor} 做 Bearer Token 鉴权，
 * 数据隔离边界 groupId 来自令牌。提供：
 * <ul>
 *   <li>{@code GET  /openapi/task/list}  —— getToDoTaskList</li>
 *   <li>{@code GET  /openapi/task/next}  —— getNextTodoTask（按创建时间排序取下一个未完成）</li>
 *   <li>{@code POST /openapi/task/checkin} —— checkInTask</li>
 * </ul>
 * 过滤条件支持 groupId（来自令牌，可选校验）、userId、parentId。
 */
@Slf4j
@RestController
@RequestMapping("/openapi/task")
public class OpenTaskController {

    @Autowired
    private TodoTaskService todoTaskService;

    @GetMapping("/list")
    public CommonResponse<List<TodoTaskDTO>> getToDoTaskList(
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        Long boundGroupId = resolveGroupId(groupId);
        return CommonResponse.success(todoTaskService.getTodoTaskList(boundGroupId, resolveUserId(userId), parentId));
    }

    @GetMapping("/next")
    public CommonResponse<TodoTaskDTO> getNextTodoTask(
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        Long boundGroupId = resolveGroupId(groupId);
        return CommonResponse.success(todoTaskService.getNextTodoTask(boundGroupId, resolveUserId(userId), parentId));
    }

    @PostMapping("/checkin")
    public CommonResponse<Long> checkInTask(@RequestBody OpenCheckinRequest request) {
        Long boundGroupId = resolveGroupId(request.getGroupId());
        Long recordId = todoTaskService.checkInTask(
                boundGroupId, request.getTaskId(), resolveUserId(request.getUserId()), request.getTaskTime());
        return CommonResponse.success(recordId);
    }

    /**
     * 解析本次请求的数据隔离 groupId：始终以令牌绑定的 groupId 为准；
     * 若调用方显式传入 groupId，则必须与令牌一致，否则拒绝（防止越权访问其它群组）。
     */
    private Long resolveGroupId(Long requestGroupId) {
        Long boundGroupId = OpenApiContext.getGroupId();
        if (boundGroupId == null) {
            // 正常情况下拦截器已保证非空；此处兜底防御。
            CommonException.LOGIN_INVALID.throwsError("访问令牌未绑定群组");
        }
        if (requestGroupId != null && !requestGroupId.equals(boundGroupId)) {
            CommonException.NOT_ALLOWED.throwsError("无权访问该群组数据");
        }
        return boundGroupId;
    }

    /**
     * 解析本次请求的 userId：未显式传入时回退到令牌绑定的成员（group_member.user_id），
     * 这样令牌就「代表该成员」操作 —— 列表/下一项默认只看该成员的任务，打卡也记到该成员名下。
     * 显式传入则按调用方意图（仍在同 group 内，由 groupId 隔离保证）。
     */
    private Long resolveUserId(Long requestUserId) {
        return requestUserId != null ? requestUserId : OpenApiContext.getUserId();
    }
}
