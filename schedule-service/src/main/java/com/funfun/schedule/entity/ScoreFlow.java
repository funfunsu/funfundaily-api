package com.funfun.schedule.entity;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

/**
 * ScoreFlow实体类，对应数据库中的score_flow表
 */
@Entity
@Table(name = "score_flow")
public class ScoreFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 记录唯一ID

    @Column(name = "score", nullable = false)
    private Integer score; // 积分数量（正数为增加，负数为减少）

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 群组ID

    @Column(name = "event_name", length = 128, nullable = false)
    private String eventName; // 事件名称

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime; // 创建时间

    @Column(name = "ext_info", columnDefinition = "JSON")
    @Type(type = "json")
    private Map<String, Object> extInfo; // 扩展信息

    @Column(name = "operator", nullable = false)
    private Long operator; // 操作人ID

    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag; // 逻辑删除：0-未删除，1-已删除

    // 构造方法
    public ScoreFlow() {
        this.createTime = new Date();
        this.deleteFlag = 0;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Map<String, Object> getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(Map<String, Object> extInfo) {
        this.extInfo = extInfo;
    }

    public Long getOperator() {
        return operator;
    }

    public void setOperator(Long operator) {
        this.operator = operator;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    @Override
    public String toString() {
        return "ScoreFlow{" +
                "id=" + id +
                ", score=" + score +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", eventName='" + eventName + '\'' +
                ", createTime=" + createTime +
                ", extInfo=" + extInfo +
                ", operator=" + operator +
                ", deleteFlag=" + deleteFlag +
                '}';
    }
}