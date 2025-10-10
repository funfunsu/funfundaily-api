package com.funfun.schedule.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * ScheduleItem实体类，对应数据库中的schedule_item表
 */
@Entity
@Table(name = "schedule_item")
@Data
public class ScheduleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID，主键，自增

    @Column(name = "item_title", length = 64)
    private String itemTitle; // 项目标题

    @Column(name = "item_desc", length = 128)
    private String itemDesc; // 项目描述

    @Column(name = "location", length = 128)
    private String location; // 位置

    @Column(name = "repeat_type", length = 32)
    private String repeatType; // 重复类型

    @Column(name = "repeat_keys", length = 128)
    private String repeatKeys; // 重复键

    @Column(name = "repeat_start_day")
    private Date repeatStartDay; // 重复开始日期

    @Column(name = "repeat_end_day")
    private Date repeatEndDay; // 重复结束日期

    @Column(name = "item_type", length = 8, nullable = false)
    private String itemType; // 项目类型，非空

    @Column(name = "start_time", nullable = false)
    private Date startTime; // 开始时间，非空

    @Column(name = "end_time", nullable = false)
    private Date endTime; // 结束时间，非空

    @Column(name = "user_id", nullable = false)
    private Long userId; // 人员ID，非空

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 组ID，非空

    // 构造方法
    public ScheduleItem() {
    }
    @Override
    public String toString() {
        return "ScheduleItem{" +
                "id=" + id +
                ", itemTitle='" + itemTitle + '\'' +
                ", itemDesc='" + itemDesc + '\'' +
                ", location='" + location + '\'' +
                ", repeatType='" + repeatType + '\'' +
                ", repeatKeys='" + repeatKeys + '\'' +
                ", repeatStartDay=" + repeatStartDay +
                ", repeatEndDay=" + repeatEndDay +
                ", itemType='" + itemType + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", userId=" + userId +
                ", groupId=" + groupId +
                '}';
    }
}