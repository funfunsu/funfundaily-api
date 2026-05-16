package com.funfun.schedule.dto;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CheckinRecordDTO {

    private Long id;

    private Long taskId;

    private Long userId;

    private Long groupId;

    private Long operatorId;

    private String taskKey;

    private LocalDateTime completeTime;

    private LocalDateTime taskTime;

    private JSONObject extra;

}
