package com.funfun.schedule.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * GroupMember实体类，对应数据库中的group_member表
 */
@Entity
@Table(name = "group_member")
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

    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag; // 逻辑删除：0-未删除，1-已删除

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime; // 创建时间

    @Column(name = "update_time", nullable = false)
    private Date updateTime; // 修改时间

    @Column(name = "inviter_id")
    private Long inviterId; // 邀请者ID

    @Column(name = "removed_id")
    private Long removedId; // 移除者ID

    // 构造方法
    public GroupMember() {
        this.deleteFlag = 0;
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

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
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

    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", role='" + role + '\'' +
                ", deleteFlag=" + deleteFlag +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", inviterId=" + inviterId +
                ", removedId=" + removedId +
                '}';
    }
}