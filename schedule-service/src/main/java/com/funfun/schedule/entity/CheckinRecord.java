package com.funfun.schedule.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkin_record")
public class CheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "complete_time", nullable = false)
    private LocalDateTime completeTime;
    @Column(name = "task_time", nullable = false)
    private LocalDateTime taskTime;

    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra; // 可以考虑使用 @Convert(converter = JsonConverter.class) 转换为 Map 或 Object

    /**
     * 任务key，形如 "${taskId}:${periodKey}"。
     * 用于识别"同一个任务的同一个周期"——daily 任务每日一个周期，所以用 task_id+date。
     */
    @Column(name = "task_key", length = 64)
    private String taskKey;

    @Column(name = "delete_flag", columnDefinition = "TINYINT")
    private Boolean deleted = false; // Hibernate 通常能很好地处理 Boolean 到 TINYINT 的映射

    // Constructors
    public CheckinRecord() {}

    public CheckinRecord(Long taskId, Long userId, Long groupId) {
        this.taskId = taskId;
        this.userId = userId;
        this.groupId = groupId;
        this.completeTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }


    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getTaskTime() {
        return taskTime;
    }

    public void setTaskTime(LocalDateTime taskTime) {
        this.taskTime = taskTime;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    @Override
    public String toString() {
        return "CheckinRecord{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", completeTime=" + completeTime +
                ", taskTime=" + taskId +
                ", extra='" + extra + '\'' +
                ", deleteFlag=" + deleted +
                '}';
    }
}