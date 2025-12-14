package com.funfun.schedule.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * ScheduleItem实体类，对应数据库中的schedule_item表
 */
@Entity
@Table(name = "schedule_item")
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


    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra; // 扩展参数
    @Column(name = "label", length = 256)
    private String label; // 标签

    @Column(name = "create_by", nullable = false)
    private Long createBy; // 人员ID，非空
    @Column(name = "update_by", nullable = false)
    private Long updateBy; // 人员ID，非空
    @Column(name = "create_time", nullable = false)
    private Date createTime; // 结束时间，非空
    @Column(name = "update_time", nullable = false)
    private Date updateTime; // 修改时间，非空

    // 构造方法
    public ScheduleItem() {
    }

    // 手动添加的getter和setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public void setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getRepeatKeys() {
        return repeatKeys;
    }

    public void setRepeatKeys(String repeatKeys) {
        this.repeatKeys = repeatKeys;
    }

    public Date getRepeatStartDay() {
        return repeatStartDay;
    }

    public void setRepeatStartDay(Date repeatStartDay) {
        this.repeatStartDay = repeatStartDay;
    }

    public Date getRepeatEndDay() {
        return repeatEndDay;
    }

    public void setRepeatEndDay(Date repeatEndDay) {
        this.repeatEndDay = repeatEndDay;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "ScheduleItem{" +
                "id=" + id +
                ", itemTitle='" + itemTitle + "'" +
                ", itemDesc='" + itemDesc + "'" +
                ", location='" + location + "'" +
                ", repeatType='" + repeatType + "'" +
                ", repeatKeys='" + repeatKeys + "'" +
                ", repeatStartDay=" + repeatStartDay +
                ", repeatEndDay=" + repeatEndDay +
                ", itemType='" + itemType + "'" +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", userId=" + userId +
                ", groupId=" + groupId +
                "}";
    }
}