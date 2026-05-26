package com.funfun.schedule.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.util.Date;

/**
 * ScheduleGroup实体类，对应数据库中的schedule_group表
 */
@Entity
@Table(name = "fun_group")
@Data
public class Group {

    // 所有字段声明在前
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 群组唯一ID

    @Column(name = "group_name", length = 64)
    private String groupName; // 群组标题

    @Column(name = "group_desc", length = 128)
    private String groupDesc; // 群组描述

    @Column(name = "type", columnDefinition = "TINYINT")
    private int type; // 类型

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime = new Date(); // 创建时间
    
    @Column(name = "creator", nullable = false)
    private Long creator; // 创建者ID

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