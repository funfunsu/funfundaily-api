package com.funfun.schedule.controller;

import com.funfun.schedule.dto.AddMemberRequest;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.User;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.GroupMemberService;
import com.funfun.schedule.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GroupMemberController类，提供群组成员相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/group/user")
public class GroupMemberController {

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    private UserService userService;

    /**
     * 用户加入群组
     */
    @PostMapping("join")
    public CommonResponse<GroupMember> joinGroup(@RequestBody GroupMember groupMember) {
        GroupMember createdMember = groupMemberService.joinGroup(groupMember);
        return CommonResponse.success(createdMember);
    }
    /**
     * 用户加入群组
     */
    @PostMapping("add")
    public CommonResponse<GroupMember> addMember(@RequestBody AddMemberRequest groupMember) {
        User user = userService.createUserByNickname(groupMember.getNickname());
        GroupMember gm = new GroupMember();
        gm.setGroupId(Long.valueOf(groupMember.getGroupId()));
        gm.setUserId(user.getId());
        GroupMember createdMember = groupMemberService.joinGroup(gm);
        return CommonResponse.success(createdMember);
    }

    /**
     * 根据ID查询群组成员
     */
    @GetMapping("/{id}")
    public CommonResponse<GroupMember> getGroupMemberById(@PathVariable Long id) {
        GroupMember member = groupMemberService.getGroupMemberById(id);
        return CommonResponse.success(member);
    }

    /**
     * 根据群组ID查询群组成员（包含用户昵称）
     */
    @GetMapping("/list")
    public CommonResponse getGroupMembersByGroupId(@RequestParam String groupId) {
        List<UserInfoDTO> members = groupMemberService.getGroupMembersWithUserInfo(Long.valueOf(groupId));
        return CommonResponse.success(members);
    }


    /**
     * 根据群组ID和用户ID查询群组成员关系
     */
    @GetMapping("/group-user")
    public CommonResponse<GroupMember> getGroupMemberByGroupIdAndUserId(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        GroupMember member = groupMemberService.getGroupMemberByGroupIdAndUserId(groupId, userId);
        return CommonResponse.success(member);
    }

    /**
     * 更新群组成员信息（如角色）
     */
    @PutMapping("/{id}")
    public CommonResponse<GroupMember> updateGroupMember(@PathVariable Long id, @RequestBody GroupMember groupMember) {
        groupMember.setId(id);
        GroupMember updatedMember = groupMemberService.updateGroupMember(groupMember);
        return CommonResponse.success(updatedMember);
    }

    /**
     * 退出群组
     */
    @DeleteMapping("/exit")
    public CommonResponse<Void> exitGroup(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        groupMemberService.exitGroup(groupId, userId);
        return CommonResponse.success();
    }

    /**
     * 移除群组成员
     */
    @DeleteMapping("/{id}")
    public CommonResponse<Void> removeGroupMember(@PathVariable Long id, @RequestParam Long removedId) {
        groupMemberService.removeGroupMember(id, removedId);
        return CommonResponse.success();
    }

    /**
     * 批量移除群组成员
     */
    @DeleteMapping("/batch")
    public CommonResponse<Void> removeGroupMembers(@RequestBody List<Long> ids, @RequestParam Long removedId) {
        groupMemberService.removeGroupMembers(ids, removedId);
        return CommonResponse.success();
    }

    /**
     * 判断用户是否在群组中
     */
    @GetMapping("/exists")
    public CommonResponse<Boolean> isUserInGroup(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        boolean isInGroup = groupMemberService.isUserInGroup(groupId, userId);
        return CommonResponse.success(isInGroup);
    }

    /**
     * 批量查询群组成员
     */
    @PostMapping("/batch/groups")
    public CommonResponse<List<GroupMember>> getGroupMembersByGroupIds(@RequestBody List<Long> groupIds) {
        List<GroupMember> members = groupMemberService.getGroupMembersByGroupIds(groupIds);
        return CommonResponse.success(members);
    }

    /**
     * 获取群组中的管理员列表
     */
    @GetMapping("/{groupId}/admins")
    public CommonResponse<List<GroupMember>> getGroupAdmins(@PathVariable Long groupId, @RequestParam String role) {
        List<GroupMember> admins = groupMemberService.getGroupAdmins(groupId, role);
        return CommonResponse.success(admins);
    }
}