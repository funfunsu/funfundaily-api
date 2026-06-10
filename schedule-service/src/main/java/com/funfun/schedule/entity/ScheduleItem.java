package com.funfun.schedule.entity;

import com.funfun.schedule.enums.CloseStatus;
import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "item_desc", columnDefinition = "TEXT")
    private String itemDesc; // 项目描述（TEXT，支持长文本；prod 迁移见 script/update_sql.sql）

    @Column(name = "location", length = 128)
    private String location; // 位置

    @Column(name = "repeat_type", length = 32)
    private String repeatType; // 重复类型

    @Column(name = "repeat_keys", length = 128)
    private String repeatKeys; // 重复键

    @Column(name = "repeat_start_day")
    private LocalDate repeatStartDay; // 重复开始日期

    @Column(name = "repeat_end_day")
    private LocalDate repeatEndDay; // 重复结束日期

    @Column(name = "item_type", length = 32, nullable = false)
    private String itemType; // 项目类型，非空（如 monthlyPlan=11 字符，留足余量）

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime; // 开始时间，非空

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime; // 结束时间，非空

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
    @Column(name = "update_scope", columnDefinition = "TEXT")
    private String updateScope;



    @Enumerated(EnumType.ORDINAL) // 或者 EnumType.STRING，根据你的存储偏好
    @Column(name = "close_status", nullable = false, columnDefinition = "TINYINT")
    private CloseStatus closeStatus = CloseStatus.OPEN; // 关闭状态，默认为OPEN
    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;


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