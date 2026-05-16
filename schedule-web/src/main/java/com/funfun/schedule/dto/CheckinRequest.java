package com.funfun.schedule.dto;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class CheckinRequest extends BaseGroupUserRequest{
    private String taskId;
    private LocalDateTime taskTime;
    private JSONObject extra; // 如果需要传递额外信息
}
