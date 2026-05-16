package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import com.funfun.schedule.enums.ScheduleItemType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetScheduleItemRequest extends BaseGroupUserRequest {
    LocalDate fromDate;
    LocalDate toDate;
    String taskId;
    List<String> taskIds;
    List<String> parentIds;

    ScheduleItemType scheduleItemType = ScheduleItemType.schedule;
}
