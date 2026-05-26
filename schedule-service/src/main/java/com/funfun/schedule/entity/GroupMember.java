package com.funfun.schedule.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.util.Date;

/**
 * GroupMember实体类，对应数据库中的group_member表
 */
@Entity
@Table(name = "group_member")
@Where(clause = "delete_flag = false")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 群组成员唯一ID

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 群组ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @Column(name = "role", length = 16)
    private String role; // 角色

    @Column(name = "delete_flag", columnDefinition = "TINYINT")
    private Boolean deleted = false; // 逻辑删除：0-未删除，1-已删除

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime; // 创建时间

    @Column(name = "update_time", nullable = false)
    private Date updateTime; // 修改时间

    @Column(name = "inviter_id")
    private Long inviterId; // 邀请者ID

    @Column(name = "removed_id")
    private Long removedId; // 移除者ID


    private int score;

    /**
     * 开放接口（OpenAPI / MCP）访问令牌。绑定到具体某个成员（一个 group_member 行一个令牌），
     * 令牌即同时确定 groupId（数据隔离边界）与归属 userId（管理维度）。
     * 改 token / 开通 / 吊销 只需更新本列，无需改配置或重启。
     * 标 @JsonIgnore 防止经由实体序列化意外泄露。
     */
    @JsonIgnore
    @Column(name = "open_api_token", length = 128)
    private String openApiToken;

    // 构造方法
    public GroupMember() {
        this.deleted = false;
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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

    public Long getInviterId() {
        return inviterId;
    }

    public void setInviterId(Long inviterId) {
        this.inviterId = inviterId;
    }

    public Long getRemovedId() {
        return removedId;
    }

    public void setRemovedId(Long removedId) {
        this.removedId = removedId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getOpenApiToken() {
        return openApiToken;
    }

    public void setOpenApiToken(String openApiToken) {
        this.openApiToken = openApiToken;
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", role='" + role + '\'' +
                ", deleteFlag=" + deleted +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", inviterId=" + inviterId +
                ", removedId=" + removedId +
                '}';
    }
}