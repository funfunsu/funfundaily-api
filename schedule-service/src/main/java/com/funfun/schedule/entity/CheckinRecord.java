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

    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra; // 可以考虑使用 @Convert(converter = JsonConverter.class) 转换为 Map 或 Object

    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag = 0; // 逻辑删除标志

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

    public Integer getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(Integer deleteFlag) { this.deleteFlag = deleteFlag; }

    @Override
    public String toString() {
        return "CheckinRecord{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", completeTime=" + completeTime +
                ", extra='" + extra + '\'' +
                ", deleteFlag=" + deleteFlag +
                '}';
    }
}