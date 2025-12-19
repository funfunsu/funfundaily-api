package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CheckinRequest extends BaseGroupUserRequest{
    private String taskId;
    private LocalDateTime taskTime;
    private Map<String,Object> extra; // 如果需要传递额外信息
}
