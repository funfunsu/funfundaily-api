package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetScheduleItemRequest extends BaseGroupUserRequest {
    LocalDateTime fromDate;
    LocalDateTime toDate;
}
