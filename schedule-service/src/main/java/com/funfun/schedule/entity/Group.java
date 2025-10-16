package com.funfun.schedule.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * ScheduleGroup实体类，对应数据库中的schedule_group表
 */
@Entity
@Table(name = "fun_group")
public class Group {

    // 所有字段声明在前
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 群组唯一ID

    @Column(name = "group_name", length = 64)
    private String groupName; // 群组标题

    @Column(name = "group_desc", length = 128)
    private String groupDesc; // 群组描述

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime; // 创建时间
    
    @Column(name = "creator", nullable = false)
    private Long creator; // 创建者ID
    
    // 构造方法
    public Group() {
        this.createTime = new Date();
    }
    
    // 手动添加的getter和setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getGroupDesc() {
        return groupDesc;
    }
    
    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
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
                ", groupName='" + groupName + "'" +
                ", groupDesc='" + groupDesc + "'" +
                ", createTime=" + createTime +
                ", creator=" + creator +
                "}";
    }
}