package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.GroupDTO;
import com.funfun.schedule.entity.Group;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.enums.GroupType;
import com.funfun.schedule.mapper.GroupMapper;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.repository.ScheduleGroupRepository;
import com.funfun.schedule.service.GroupMemberService;
import com.funfun.schedule.service.ScheduleGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduleGroupService接口的实现类，实现群组相关的业务逻辑
 */
@Service
public class ScheduleGroupServiceImpl implements ScheduleGroupService {

    @Autowired
    private ScheduleGroupRepository scheduleGroupRepository;


    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupMapper groupMapper;
    @Override
    @Transactional
    public Group createGroup(GroupDTO group) {
        // 检查群组标题是否为空
        if (group.getGroupName() == null || group.getGroupName().trim().isEmpty()) {
            throw new RuntimeException("群组标题不能为空");
        }
        // 检查创建者ID是否为空
        if (group.getCreator() == null) {
            throw new RuntimeException("创建者ID不能为空");
        }
        // 检查是否已存在同名群组
        Optional<Group> existingGroup = scheduleGroupRepository.findByGroupNameAndCreator(group.getGroupName(), group.getCreator());
        if (existingGroup.isPresent()) {
            throw new RuntimeException("您已创建同名群组");
        }
        // 设置创建时间
        if (group.getCreateTime() == null) {
            group.setCreateTime(new Date());
        }

        return scheduleGroupRepository.save(groupMapper.toEntity(group));
    }


    @Override
    public Group createAutoGroup(Long userId) {
        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setCreator(userId);
        groupDTO.setType(GroupType.Auto.ordinal());
        groupDTO.setGroupName("我的空间");
        groupDTO.setGroupDesc("自动创建");
        return scheduleGroupRepository.save(groupMapper.toEntity(groupDTO));
    }

    @Override
    public Group getGroupById(Long id) {
        Optional<Group> optionalGroup = scheduleGroupRepository.findById(id);
        return optionalGroup.orElseThrow(() -> new RuntimeException("群组不存在"));
    }


    @Override
    public List<Group> getGroupList(Long userId) {
        List<GroupMember> groupMemberList = groupMemberRepository.findByUserId(userId);
        if (groupMemberList == null || groupMemberList.isEmpty()){
            return Collections.EMPTY_LIST;
        }
        List<Long> groupIds = groupMemberList.stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        return scheduleGroupRepository.findByIdIn(groupIds);
    }

    @Override
    public List<Group> getGroupsByCreator(Long creator) {
        return scheduleGroupRepository.findByCreator(creator);
    }

    @Override
    public Group getOrCreateGroupByUser(Long creator) {
        //先查找他创建的
        List<Group> groupList = getGroupsByCreator(creator);
        if (groupList != null && !groupList.isEmpty()){
            return groupList.get(0);
        }
        //再查找他所在的
        groupList = getGroupList(creator);
        if (groupList != null && !groupList.isEmpty()){
            return groupList.get(0);
        }
        return createAutoGroup(creator);
    }

    @Override
    public List<Group> getGroupsByTitleContaining(String title) {
        return scheduleGroupRepository.findByGroupNameContaining(title);
    }

    @Override
    public List<Group> getGroupsByCreateTimeBetween(Date startDate, Date endDate) {
        return scheduleGroupRepository.findByCreateTimeBetween(startDate, endDate);
    }

    @Override
    @Transactional
    public Group updateGroup(GroupDTO groupDTO) {
        // 检查群组是否存在
        Group existingGroup = getGroupById(groupDTO.getId());
        // 检查是否已存在同名群组（排除当前群组）
        Optional<Group> sameNameGroup = scheduleGroupRepository.findByGroupNameAndCreator(groupDTO.getGroupName(), existingGroup.getCreator());
        if (sameNameGroup.isPresent() && !sameNameGroup.get().getId().equals(groupDTO.getId())) {
            throw new RuntimeException("您已创建同名群组");
        }
        // 更新群组信息
        existingGroup.setGroupName(groupDTO.getGroupName());
        existingGroup.setGroupDesc(groupDTO.getGroupDesc());
        return scheduleGroupRepository.save(existingGroup);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        // 检查群组是否存在
        Group group = getGroupById(id);
        // 删除群组（物理删除，也可以根据需求改为逻辑删除）
        scheduleGroupRepository.delete(group);
    }

    @Override
    @Transactional
    public void deleteGroups(List<Long> ids) {
        // 批量删除群组
        scheduleGroupRepository.deleteByIdIn(ids);
    }

    @Override
    public List<Group> getGroupsByIds(List<Long> ids) {
        return scheduleGroupRepository.findByIdIn(ids);
    }

    @Override
    public Group getGroupByTitleAndCreator(String title, Long creator) {
        Optional<Group> optionalGroup = scheduleGroupRepository.findByGroupNameAndCreator(title, creator);
        return optionalGroup.orElseThrow(() -> new RuntimeException("群组不存在"));
    }
}