package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.GroupMemberDTO;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.Group;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.User;
import com.funfun.schedule.enums.BindType;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.mapper.GroupMemberMapper;
import com.funfun.schedule.mapper.UserMapper;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.service.GroupMemberService;
import com.funfun.schedule.service.ScheduleGroupService;
import com.funfun.schedule.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GroupMemberService接口的实现类，实现群组成员相关的业务逻辑
 */
@Slf4j
@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private ScheduleGroupService groupService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public GroupMember joinGroup(GroupMember groupMember) {
        // 检查群组ID和用户ID是否为空
        if (groupMember.getGroupId() == null || groupMember.getUserId() == null) {
            CommonException.DATA_INVALID.throwsError("群组ID和用户ID不能为空");
        }
        // 检查用户是否已在群组中
        if (isUserInGroup(groupMember.getGroupId(), groupMember.getUserId())) {
            CommonException.DATA_DUPLICATE.throwsError("用户已在群组中");
        }

        //清理自动创建的群组
        removeAutoGroup(groupMember.getUserId());

        // 设置默认值
        if (groupMember.getRole() == null) {
            groupMember.setRole(GroupRole.Member.name()); // 默认为普通成员
        }
        groupMember.setDeleted(false);
        if (groupMember.getCreateTime() == null) {
            groupMember.setCreateTime(new Date());
        }
        if (groupMember.getUpdateTime() == null) {
            groupMember.setUpdateTime(new Date());
        }
        return groupMemberRepository.save(groupMember);
    }

    private void removeAutoGroup(Long userId){
        Group group = groupService.removeAutoGroup(userId);
        if (group == null){
            return;
        }
        List<GroupMember> list = getGroupMembersByGroupId(group.getId());
        if (list == null || list.size() != 1){
            log.info("removeAutoGroup skip member.size <> 1,{}",userId);
            return;
        }
        doRemoveGroupMember(list.get(0).getId(),userId);
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
        GroupMember existingMember = getGroupMemberByGroupIdAndUserId(groupMember.getGroupId(),groupMember.getUserId());
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
        member.setDeleted(true);
        member.setUpdateTime(new Date());
        groupMemberRepository.save(member);
    }

    private void doRemoveGroupMember(Long id, Long removedId) {
        // 检查群组成员是否存在
        GroupMember member = getGroupMemberById(id);
        // 逻辑删除
        member.setDeleted(true);
        member.setRemovedId(removedId);
        member.setUpdateTime(new Date());
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeGroupMember(Long groupId, Long removedId) {
        GroupMember member = getGroupMemberByGroupIdAndUserId(groupId,removedId);
        if (member == null){
            return;
        }
        doRemoveGroupMember(member.getId(),removedId);
    }

    @Override
    @Transactional
    public void removeGroupMembers(List<Long> ids, Long removedId) {
        // 批量逻辑删除群组成员
        List<GroupMember> members = groupMemberRepository.findAllById(ids);
        for (GroupMember member : members) {
            member.setDeleted(true);
            member.setRemovedId(removedId);
            member.setUpdateTime(new Date());
        }
        groupMemberRepository.saveAll(members);
    }

    @Override
    public boolean isUserInGroup(Long groupId, Long userId) {
        Optional<GroupMember> optionalMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        return optionalMember.isPresent() && !optionalMember.get().getDeleted();
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
    public List<GroupMemberDTO> getGroupMembersWithUserInfo(Long groupId) {
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

        List<GroupMemberDTO> results = groupMemberMapper.toDTOList(members);
        
        // 批量查询用户信息
        List<User> users = userService.getUsersByIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        results.forEach(item ->{
            User user = userMap.get(item.getUserId());
            item.setUserInfo(userMapper.toSimpleDTO(user));
            if (user.getOpenid() != null && !user.getOpenid().isBlank()){
                item.setBindType(BindType.BindWithWx.name());
            }else{
                item.setBindType(BindType.None.name());
            }
        });
        return results;
    }


    @Override
    public void updateMemberScore(Long groupId, Long userId, int newBalance) {
        // 检查用户是否在群组中
        GroupMember member = getGroupMemberByGroupIdAndUserId(groupId, userId);
        member.setScore(newBalance);
        member.setUpdateTime(new Date());
        groupMemberRepository.save(member);
    }

    @Override
    public int getMemberScore(Long groupId, Long userId) {
        GroupMember member = getGroupMemberByGroupIdAndUserId(groupId, userId);
        return member.getScore();
    }

    @Override
    public int includeMemberCount(Long groupId) {
        return (int)groupMemberRepository.countByGroupId(groupId);
    }
}