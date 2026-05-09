package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
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
    private LocalDateTime repeatStartDay;

    /**
     * 重复结束日期
     */
    private LocalDateTime repeatEndDay;

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


    private Map<String,Object> extra;

    /**
     * 后端按当日 + RepeatType 计算出的展示辅助字段：
     *   itemKey         当前展示日所对应的"周期实例"键 (eg. "${id}:${yyyy-MM-dd}")
     *   lastCompleteKey 最近一次完成对应的同格式键
     *   dueDate         排序用的截止日期 (yyyy-MM-dd)
     */
    private Map<String,Object> showExtra;

    /**
     * 任务运行期快照（持久化在 schedule_item.update_scope 列）。
     * 主要用于 isTaskUndo 判断"今天刚完成的任务仍然展示"。
     */
    private ScheduleItemUpdateScope updateScope;

    /** 关联 parentItemId（任务隶属的目标 itemId） */
    private Long parentId;

    /** 完成状态 */
    private Integer closeStatus;
}