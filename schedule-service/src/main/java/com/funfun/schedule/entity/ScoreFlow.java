package com.funfun.schedule.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "score_flow")
public class ScoreFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flow_type", nullable = false, columnDefinition = "TINYINT")
    private Integer flowType; // 0 - 入账, 1 - 出账

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "balance", nullable = false)
    private Integer balance; // 流水发生后的余额快照

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "event_name", length = 128, nullable = false)
    private String eventName;

    @Column(name = "label", length = 256)
    private String label;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra; // 扩展参数

    @Column(name = "operator", nullable = false)
    private Long operator; // 操作人ID

    @Column(name = "delete_flag", columnDefinition = "TINYINT")
    private Boolean deleted = false; // Hibernate 通常能很好地处理 Boolean 到 TINYINT 的映射    private Integer deleteFlag = 0; // 逻辑删除标志

    // Constructors
    public ScoreFlow() {}

    public ScoreFlow(Integer flowType, Integer score, Integer remainScore, Long userId, Long groupId, String eventName, String label, Long operator) {
        this.flowType = flowType;
        this.score = score;
        this.balance = remainScore;
        this.userId = userId;
        this.groupId = groupId;
        this.eventName = eventName;
        this.label = label;
        this.operator = operator;
        this.createTime = LocalDateTime.now();
    }


    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getFlowType() { return flowType; }
    public void setFlowType(Integer flowType) { this.flowType = flowType; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    public Long getOperator() { return operator; }
    public void setOperator(Long operator) { this.operator = operator; }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "ScoreFlow{" +
                "id=" + id +
                ", flowType=" + flowType +
                ", score=" + score +
                ", balance=" + balance +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", eventName='" + eventName + '\'' +
                ", label='" + label + '\'' +
                ", createTime=" + createTime +
                ", extra='" + extra + '\'' +
                ", operator=" + operator +
                ", deleteFlag=" + deleted +
                '}';
    }
}