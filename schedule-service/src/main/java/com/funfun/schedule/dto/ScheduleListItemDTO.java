package com.funfun.schedule.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.funfun.schedule.entity.ScheduleItem;
import lombok.Data;

import java.util.List;

/**
 * 日程项DTO（数据传输对象）
 * 用于前端与后端之间的数据交互
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleListItemDTO {
    String date;
    List<ScheduleItem> schedules;

    public ScheduleListItemDTO(String date, List<ScheduleItem> schedules) {
        this.date = date;
        this.schedules = schedules;
    }
}
