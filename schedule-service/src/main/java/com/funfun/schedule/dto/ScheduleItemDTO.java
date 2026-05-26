package com.funfun.schedule.dto;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.enums.RepeatType;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程项DTO（数据传输对象）
 * 用于前端与后端之间的数据交互
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleItemDTO {

    /**
     * 日程项ID
     */
    private Long id;

    /**
     * 日程标题
     */
    private String itemTitle;

    /**
     * 日程描述
     */
    private String itemDesc;

    /**
     * 地点
     */
    private String location;

    /**
     * 重复类型：none（不重复）、daily（每日）、weekly（每周）、'oddWeek' 单周,'evenWeek' 双周、monthly（每月）
     */
    private RepeatType repeatType;

    /**
     * 重复规则键值列表，如["1","2"]表示周一、周二
     */
    private List<String> repeatKeys;

    /**
     * 重复开始日期
     */
    private LocalDate repeatStartDay;

    /**
     * 重复结束日期
     */
    private LocalDate repeatEndDay;

    /**
     * 日程类型：meeting（会议）、task（任务）等
     */
    private String itemType;


    /**
     * 日程类型：meeting（会议）、task（任务）等
     */
    private String label;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    private Long userId;


    private JSONObject extra;
    private ScheduleItemUpdateScope updateScope;


    private CloseStatus closeStatus;
    private Long parentId = 0L;

    /**
     * 仅供前端展示
     */
    private JSONObject showExtra;
}