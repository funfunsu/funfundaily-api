package com.funfun.schedule.dto;

import com.funfun.schedule.enums.ScheduleItemType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class QueryScheduleItemDTO{
    LocalDate fromDate;
    LocalDate toDate;
    String taskId;
    List<Long> taskIds;
    List<Long> parentIds;
    ScheduleItemType scheduleItemType = ScheduleItemType.schedule;
}
