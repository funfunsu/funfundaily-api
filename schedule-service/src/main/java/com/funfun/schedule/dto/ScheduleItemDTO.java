package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * 重复类型：none（不重复）、daily（每日）、weekly（每周）、monthly（每月）
     */
    private String repeatType;

    /**
     * 重复规则键值列表，如["1","2"]表示周一、周二
     */
    private List<String> repeatKeys;

    /**
     * 重复开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date repeatStartDay;

    /**
     * 重复结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date repeatEndDay;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date endTime;


    private Map<String,Object> extra;
}