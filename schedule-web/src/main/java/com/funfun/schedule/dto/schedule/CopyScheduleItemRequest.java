package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import lombok.Data;

@Data
public class CopyScheduleItemRequest extends BaseGroupUserRequest {
    public String shareToken;
}
