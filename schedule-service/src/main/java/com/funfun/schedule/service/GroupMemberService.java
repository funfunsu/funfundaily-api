package com.funfun.schedule.service;

import com.funfun.schedule.dto.GroupMemberDTO;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.GroupMember;

import java.util.List;

/**
 * GroupMemberService接口，定义群组成员相关的业务逻辑方法
 */
public interface GroupMemberService {

    /**
     * 用户加入群组
     * @param groupMember 群组成员对象
     * @return 群组成员对象
     */
    GroupMember joinGroup(GroupMember groupMember);

    /**
     * 根据ID查询群组成员
     * @param id 群组成员ID
     * @return 群组成员对象
     */
    GroupMember getGroupMemberById(Long id);

    /**
     * 根据群组ID查询群组成员
     * @param groupId 群组ID
     * @return 群组成员列表
     */
    List<GroupMember> getGroupMembersByGroupId(Long groupId);

    /**
     * 根据用户ID查询该用户加入的群组
     * @param userId 用户ID
     * @return 群组成员列表
     */
    List<GroupMember> getGroupMembersByUserId(Long userId);

    /**
     * 根据群组ID和用户ID查询群组成员关系
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 群组成员对象
     */
    GroupMember getGroupMemberByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 更新群组成员信息（如角色）
     * @param groupMember 群组成员对象
     * @return 更新后的群组成员对象
     */
    GroupMember updateGroupMember(GroupMember groupMember);

    /**
     * 退出群组（逻辑删除）
     * @param groupId 群组ID
     * @param userId 用户ID
     */
    void exitGroup(Long groupId, Long userId);

    /**
     * 移除群组成员（逻辑删除）
     * @param groupId 群组成员ID
     * @param removedId 移除者ID
     */
    void removeGroupMember(Long groupId, Long removedId);

    /**
     * 批量移除群组成员
     * @param ids 群组成员ID列表
     * @param removedId 移除者ID
     */
    void removeGroupMembers(List<Long> ids, Long removedId);

    /**
     * 判断用户是否在群组中
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否在群组中
     */
    boolean isUserInGroup(Long groupId, Long userId);

    /**
     * 批量查询群组成员
     * @param groupIds 群组ID列表
     * @return 群组成员列表
     */
    List<GroupMember> getGroupMembersByGroupIds(List<Long> groupIds);

    /**
     * 获取群组中的管理员列表
     * @param groupId 群组ID
     * @param role 角色
     * @return 管理员列表
     */
    List<GroupMember> getGroupAdmins(Long groupId, String role);
    
    /**
     * 根据群组ID查询群组成员（包含用户昵称）
     * @param groupId 群组ID
     * @return 包含用户昵称的群组成员列表
     */
    List<GroupMemberDTO> getGroupMembersWithUserInfo(Long groupId);

    /**
     * 更新用户积分
     * @param groupId
     * @param userId
     * @param newBalance
     */
    void updateMemberScore(Long groupId, Long userId, int newBalance);
    int getMemberScore(Long groupId, Long userId);
}