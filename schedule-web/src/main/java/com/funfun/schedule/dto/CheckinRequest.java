package com.funfun.schedule.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CheckinRequest {
    private String taskId;
    private String groupId;
    private String userId; // 通常由后端从安全上下文获取
    private Map<String,Object> extra; // 如果需要传递额外信息
}
