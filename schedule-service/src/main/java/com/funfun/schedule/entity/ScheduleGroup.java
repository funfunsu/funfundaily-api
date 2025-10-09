package com.funfun.schedule.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * ScheduleGroup实体类，对应数据库中的schedule_group表
 */
@Entity
@Table(name = "schedule_group")
public class ScheduleGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 群组唯一ID

    @Column(name = "item_title", length = 64)
    private String itemTitle; // 群组标题

    @Column(name = "item_desc", length = 128)
    private String itemDesc; // 群组描述

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime; // 创建时间

    @Column(name = "creator", nullable = false)
    private Long creator; // 创建者ID

    // 构造方法
    public ScheduleGroup() {
        this.createTime = new Date();
    }

    // Getter和Setter方法
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    @Override
    public String toString() {
        return "ScheduleGroup{" +
                "id=" + id +
                ", itemTitle='" + itemTitle + '\'' +
                ", itemDesc='" + itemDesc + '\'' +
                ", createTime=" + createTime +
                ", creator=" + creator +
                '}';
    }
}