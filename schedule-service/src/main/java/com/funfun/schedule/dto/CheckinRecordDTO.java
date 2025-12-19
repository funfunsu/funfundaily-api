package com.funfun.schedule.dto;

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

    private LocalDateTime completeTime;

    private LocalDateTime taskTime;

    private Map<String,Object> extra;

}
