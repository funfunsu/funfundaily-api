package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.User;
import com.funfun.schedule.mapper.UserMapper;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.service.GroupMemberService;
import com.funfun.schedule.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GroupMemberService接口的实现类，实现群组成员相关的业务逻辑
 */
@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public GroupMember joinGroup(GroupMember groupMember) {
        // 检查群组ID和用户ID是否为空
        if (groupMember.getGroupId() == null || groupMember.getUserId() == null) {
            throw new RuntimeException("群组ID和用户ID不能为空");
        }
        // 检查用户是否已在群组中
        if (isUserInGroup(groupMember.getGroupId(), groupMember.getUserId())) {
            throw new RuntimeException("用户已在群组中");
        }
        // 设置默认值
        if (groupMember.getRole() == null) {
            groupMember.setRole("member"); // 默认为普通成员
        }
        if (groupMember.getDeleteFlag() == null) {
            groupMember.setDeleteFlag(0); // 默认为未删除
        }
        if (groupMember.getCreateTime() == null) {
            groupMember.setCreateTime(new Date());
        }
        if (groupMember.getUpdateTime() == null) {
            groupMember.setUpdateTime(new Date());
        }
        return groupMemberRepository.save(groupMember);
    }

    @Override
    public GroupMember getGroupMemberById(Long id) {
        Optional<GroupMember> optionalMember = groupMemberRepository.findById(id);
        return optionalMember.orElseThrow(() -> new RuntimeException("群组成员不存在"));
    }

    @Override
    public List<GroupMember> getGroupMembersByGroupId(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    @Override
    public List<GroupMember> getGroupMembersByUserId(Long userId) {
        return groupMemberRepository.findByUserId(userId);
    }

    @Override
    public GroupMember getGroupMemberByGroupIdAndUserId(Long groupId, Long userId) {
        Optional<GroupMember> optionalMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        return optionalMember.orElseThrow(() -> new RuntimeException("用户不在该群组中"));
    }

    @Override
    @Transactional
    public GroupMember updateGroupMember(GroupMember groupMember) {
        // 检查群组成员是否存在
        GroupMember existingMember = getGroupMemberById(groupMember.getId());
        // 更新信息
        existingMember.setRole(groupMember.getRole());
        existingMember.setUpdateTime(new Date());
        return groupMemberRepository.save(existingMember);
    }

    @Override
    @Transactional
    public void exitGroup(Long groupId, Long userId) {
        // 检查用户是否在群组中
        GroupMember member = getGroupMemberByGroupIdAndUserId(groupId, userId);
        // 逻辑删除
        member.setDeleteFlag(1);
        member.setUpdateTime(new Date());
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeGroupMember(Long id, Long removedId) {
        // 检查群组成员是否存在
        GroupMember member = getGroupMemberById(id);
        // 逻辑删除
        member.setDeleteFlag(1);
        member.setRemovedId(removedId);
        member.setUpdateTime(new Date());
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeGroupMembers(List<Long> ids, Long removedId) {
        // 批量逻辑删除群组成员
        List<GroupMember> members = groupMemberRepository.findAllById(ids);
        for (GroupMember member : members) {
            member.setDeleteFlag(1);
            member.setRemovedId(removedId);
            member.setUpdateTime(new Date());
        }
        groupMemberRepository.saveAll(members);
    }

    @Override
    public boolean isUserInGroup(Long groupId, Long userId) {
        Optional<GroupMember> optionalMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        return optionalMember.isPresent() && optionalMember.get().getDeleteFlag() == 0;
    }

    @Override
    public List<GroupMember> getGroupMembersByGroupIds(List<Long> groupIds) {
        return groupMemberRepository.findByGroupIdIn(groupIds);
    }

    @Override
    public List<GroupMember> getGroupAdmins(Long groupId, String role) {
        return groupMemberRepository.findByGroupIdAndRole(groupId, role);
    }

    @Override
    public List<UserInfoDTO> getGroupMembersWithUserInfo(Long groupId) {
        // 查询指定群组的所有成员
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        
        if (members.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 收集所有用户ID
        List<Long> userIds = members.stream()
                .map(GroupMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询用户信息
        List<User> users = userService.getUsersByIds(userIds);
        users.forEach(user -> {
            if (user.getNickname() == null ||user.getNickname().isBlank()){
                user.setNickname("默认");
            }
        });

        return userMapper.toSimpleList(users);
    }
}