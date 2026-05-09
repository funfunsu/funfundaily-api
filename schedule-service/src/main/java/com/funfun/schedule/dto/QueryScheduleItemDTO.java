package com.funfun.schedule.dto;

import com.funfun.schedule.enums.ScheduleItemType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内部封装的日程查询参数：
 *   - 命中 taskIds：直接按 id 列表取
 *   - 命中 parentIds：按目标 id 列表取（依赖 ScheduleItem.parentId 列）
 *   - 否则：按 fromDate/toDate + scheduleItemType 走重叠查询
 */
@Data
public class QueryScheduleItemDTO {
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private ScheduleItemType scheduleItemType;
    private List<Long> taskIds;
    private List<Long> parentIds;
}
