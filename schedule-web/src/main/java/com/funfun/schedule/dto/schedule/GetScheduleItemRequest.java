package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import com.funfun.schedule.enums.ScheduleItemType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetScheduleItemRequest extends BaseGroupUserRequest {
    LocalDateTime fromDate;
    LocalDateTime toDate;
    /**
     * 可选过滤条件：要查询的事项类型。
     * - 不传：保持历史行为（不同入口由 Controller 设默认值）
     * - 传：透传给 ScheduleItemService.getScheduleItemsByDateRange
     */
    ScheduleItemType scheduleItemType;
    /** 可选：单个 taskId（CheckinRecord 查询用） */
    String taskId;
    /** 可选：批量 taskIds，命中即按 id 列表查询 */
    java.util.List<String> taskIds;
    /** 可选：批量 parentIds（按"目标"分组查询子任务） */
    java.util.List<String> parentIds;
}
