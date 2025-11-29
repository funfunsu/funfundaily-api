package com.funfun.schedule.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

/**
 * CheckinRecord实体类，对应数据库中的checkin_record表
 */
@Entity
@Table(name = "checkin_record")
public class CheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 记录唯一ID

    @Column(name = "task_id", nullable = false)
    private Long taskId; // 任务ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 群组ID

    @Column(name = "complete_status", nullable = false,columnDefinition = "TINYINT")
    private Integer completeStatus; // 完成状态

    @Column(name = "complete_time", nullable = false, updatable = false)
    private Date completeTime; // 完成时间

    @Column(name = "ext_info", columnDefinition = "JSON")
    @Type(type = "json")
    private JsonNode extInfo; // 扩展信息

    @Column(name = "delete_flag", nullable = false,columnDefinition = "TINYINT")
    private Integer deleteFlag; // 逻辑删除：0-未删除，1-已删除

    // 构造方法
    public CheckinRecord() {
        this.completeTime = new Date();
        this.deleteFlag = 0;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public Integer getCompleteStatus() {
        return completeStatus;
    }

    public void setCompleteStatus(Integer completeStatus) {
        this.completeStatus = completeStatus;
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public JsonNode getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(JsonNode extInfo) {
        this.extInfo = extInfo;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    @Override
    public String toString() {
        return "CheckinRecord{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", completeStatus=" + completeStatus +
                ", completeTime=" + completeTime +
                ", extInfo=" + extInfo +
                ", deleteFlag=" + deleteFlag +
                '}';
    }
}