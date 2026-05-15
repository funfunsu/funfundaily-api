package com.funfun.schedule.dto;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UniversalRecordDTO {
    private Long id;
    private String scene;
    private String sceneVar; // 作为字符串存储
    private String businessKey;
    private String content; // 使用 String 存储 JSON
    private JSONObject extra; // 使用 String 存储 JSON
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
}

