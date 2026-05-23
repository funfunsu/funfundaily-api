package com.funfun.schedule.service;

import com.funfun.schedule.dto.TodoTaskDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放接口（OpenAPI / MCP）待办任务服务。
 *
 * <p>所有方法都以 groupId 作为强制数据隔离边界（由 Bearer Token 解析得到），
 * userId / parentId 为可选过滤条件。任务即 itemType="task" 的 schedule_item 记录。
 */
public interface TodoTaskService {

    /**
     * 查询群组下的待办任务列表，按创建时间升序。
     *
     * @param groupId  群组 ID（数据隔离边界，必填）
     * @param userId   成员 ID（可选过滤）
     * @param parentId 父任务 ID（可选过滤）
     */
    List<TodoTaskDTO> getTodoTaskList(Long groupId, Long userId, Long parentId);

    /**
     * 获取下一个待办任务：按创建时间升序排列后，返回第一个当前周期尚未完成的任务。
     * 若全部已完成则返回 {@code null}。
     */
    TodoTaskDTO getNextTodoTask(Long groupId, Long userId, Long parentId);

    /**
     * 对某个任务执行打卡 / 完成。
     *
     * @param groupId  群组 ID（必须与任务所属群组一致，否则拒绝）
     * @param taskId   任务 ID
     * @param userId   打卡成员 ID；为空时默认任务归属成员
     * @param taskTime 打卡所属时间；为空时默认当天
     * @return 生成的打卡记录 ID
     */
    Long checkInTask(Long groupId, Long taskId, Long userId, LocalDateTime taskTime);
}
