package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.TodoTaskDTO;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.mapper.ScheduleItemMapper;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.service.CheckinService;
import com.funfun.schedule.service.ScheduleItemService;
import com.funfun.schedule.service.TodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link TodoTaskService} 实现。
 *
 * <p>任务即 itemType="task" 的 schedule_item 记录；当前周期的完成进度复用打卡
 * （CheckinRecord）的 taskKey 计数逻辑，与小程序端打卡保持一致。
 */
@Slf4j
@Service
public class TodoTaskServiceImpl implements TodoTaskService {

    private static final String TASK_ITEM_TYPE = ScheduleItemType.task.name();
    // 与 extra JSON 中的键对齐（同 CheckinServiceImpl）。
    private static final String EXTRA_TASK_TYPE_KEY = "taskType";
    private static final String EXTRA_TOTAL_COUNT_KEY = "totalCount";

    private final ScheduleItemRepository scheduleItemRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final ScheduleItemMapper scheduleItemMapper;
    private final ScheduleItemService scheduleItemService;
    private final CheckinService checkinService;

    @Autowired
    public TodoTaskServiceImpl(ScheduleItemRepository scheduleItemRepository,
                               CheckinRecordRepository checkinRecordRepository,
                               ScheduleItemMapper scheduleItemMapper,
                               ScheduleItemService scheduleItemService,
                               CheckinService checkinService) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.checkinRecordRepository = checkinRecordRepository;
        this.scheduleItemMapper = scheduleItemMapper;
        this.scheduleItemService = scheduleItemService;
        this.checkinService = checkinService;
    }

    @Override
    public List<TodoTaskDTO> getTodoTaskList(Long groupId, Long userId, Long parentId) {
        if (groupId == null) {
            CommonException.PARAM_INVALID.throwsError("groupId 不能为空");
        }
        List<ScheduleItem> items =
                scheduleItemRepository.findOpenTasksForOpenApi(groupId, TASK_ITEM_TYPE, userId, parentId);
        LocalDate today = LocalDate.now();
        List<TodoTaskDTO> result = new ArrayList<>(items.size());
        for (ScheduleItem item : items) {
            result.add(toTodoTaskDTO(item, today));
        }
        return result;
    }

    @Override
    public TodoTaskDTO getNextTodoTask(Long groupId, Long userId, Long parentId) {
        // 已按创建时间升序返回，取第一个「当前周期未完成」的任务。
        for (TodoTaskDTO task : getTodoTaskList(groupId, userId, parentId)) {
            if (!Boolean.TRUE.equals(task.getCompleted())) {
                return task;
            }
        }
        return null;
    }

    @Override
    public Long checkInTask(Long groupId, Long taskId, Long userId, LocalDateTime taskTime) {
        if (groupId == null) {
            CommonException.PARAM_INVALID.throwsError("groupId 不能为空");
        }
        if (taskId == null) {
            CommonException.PARAM_INVALID.throwsError("taskId 不能为空");
        }
        ScheduleItem task = scheduleItemRepository.findById(taskId).orElse(null);
        if (task == null) {
            CommonException.DATA_NOT_EXIST.throwsError();
        }
        // 数据隔离：任务必须归属当前令牌绑定的群组。
        if (!groupId.equals(task.getGroupId())) {
            CommonException.NOT_ALLOWED.throwsError("任务不属于当前群组");
        }
        Long targetUserId = userId != null ? userId : task.getUserId();

        CheckinRecordDTO recordDTO = new CheckinRecordDTO();
        recordDTO.setGroupId(groupId);
        recordDTO.setUserId(targetUserId);
        recordDTO.setTaskId(taskId);
        recordDTO.setOperatorId(targetUserId);
        recordDTO.setTaskTime(taskTime);
        return checkinService.performCheckin(recordDTO);
    }

    /** 实体 -> 开放接口视图，并补充当前周期完成进度。 */
    private TodoTaskDTO toTodoTaskDTO(ScheduleItem item, LocalDate today) {
        ScheduleItemDTO dto = scheduleItemMapper.toDTO(item);

        TodoTaskDTO out = new TodoTaskDTO();
        out.setId(item.getId());
        out.setTitle(item.getItemTitle());
        out.setContent(item.getItemDesc());
        out.setGroupId(item.getGroupId());
        out.setUserId(item.getUserId());
        out.setParentId(item.getParentId());
        out.setRepeatType(item.getRepeatType());
        out.setCreateTime(item.getCreateTime());

        JSONObject extra = dto.getExtra();
        Integer totalCount = null;
        if (extra != null) {
            out.setTaskType(extra.getString(EXTRA_TASK_TYPE_KEY));
            totalCount = extra.getInteger(EXTRA_TOTAL_COUNT_KEY);
        }
        out.setTotalCount(totalCount);

        // 完成进度按任务归属成员（item.userId）统计当前周期已打卡次数。
        Integer completedCount = computeCompletedCount(dto, item.getGroupId(), item.getUserId(), today);
        out.setCompletedCount(completedCount);
        out.setCompleted(isCompleted(completedCount, totalCount));
        return out;
    }

    /** 计算当前周期已完成次数；无法判定（缺少重复类型）时返回 null。 */
    private Integer computeCompletedCount(ScheduleItemDTO dto, Long groupId, Long userId, LocalDate today) {
        if (userId == null || dto.getRepeatType() == null) {
            return null;
        }
        String taskKey = scheduleItemService.getTaskKey(dto, today);
        return checkinRecordRepository.countByGroupIdAndUserIdAndTaskKey(groupId, userId, taskKey);
    }

    /**
     * 是否已完成：
     * <ul>
     *   <li>设置了 totalCount(&gt;0)：已完成次数达到目标即完成；</li>
     *   <li>未设置 totalCount：至少打卡一次即视为完成；</li>
     *   <li>无法统计（completedCount 为 null）：返回 null（未知）。</li>
     * </ul>
     */
    private Boolean isCompleted(Integer completedCount, Integer totalCount) {
        if (completedCount == null) {
            return null;
        }
        if (totalCount != null && totalCount > 0) {
            return completedCount >= totalCount;
        }
        return completedCount > 0;
    }
}
