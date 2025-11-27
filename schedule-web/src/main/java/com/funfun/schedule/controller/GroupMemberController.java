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
    public ResponseEntity<GroupMember> joinGroup(@RequestBody GroupMember groupMember) {
        GroupMember createdMember = groupMemberService.joinGroup(groupMember);
        return new ResponseEntity<>(createdMember, HttpStatus.CREATED);
    }
    /**
     * 用户加入群组
     */
    @PostMapping("add")
    public ResponseEntity<GroupMember> addMember(@RequestBody AddMemberRequest groupMember) {
        User user = userService.createUserByNickname(groupMember.getNickname());
        GroupMember gm = new GroupMember();
        gm.setGroupId(Long.valueOf(groupMember.getGroupId()));
        gm.setUserId(user.getId());
        GroupMember createdMember = groupMemberService.joinGroup(gm);
        return new ResponseEntity<>(createdMember, HttpStatus.CREATED);
    }

    /**
     * 根据ID查询群组成员
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupMember> getGroupMemberById(@PathVariable Long id) {
        GroupMember member = groupMemberService.getGroupMemberById(id);
        return ResponseEntity.ok(member);
    }

    /**
     * 根据群组ID查询群组成员（包含用户昵称）
     */
    @GetMapping("/list")
    public ResponseEntity<List<UserInfoDTO>> getGroupMembersByGroupId(@RequestParam String groupId) {
        List<UserInfoDTO> members = groupMemberService.getGroupMembersWithUserInfo(Long.valueOf(groupId));
        return ResponseEntity.ok(members);
    }

    /**
     * 根据用户ID查询该用户加入的群组
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupMember>> getGroupMembersByUserId(@PathVariable Long userId) {
        List<GroupMember> members = groupMemberService.getGroupMembersByUserId(userId);
        return ResponseEntity.ok(members);
    }

    /**
     * 根据群组ID和用户ID查询群组成员关系
     */
    @GetMapping("/group-user")
    public ResponseEntity<GroupMember> getGroupMemberByGroupIdAndUserId(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        GroupMember member = groupMemberService.getGroupMemberByGroupIdAndUserId(groupId, userId);
        return ResponseEntity.ok(member);
    }

    /**
     * 更新群组成员信息（如角色）
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupMember> updateGroupMember(@PathVariable Long id, @RequestBody GroupMember groupMember) {
        groupMember.setId(id);
        GroupMember updatedMember = groupMemberService.updateGroupMember(groupMember);
        return ResponseEntity.ok(updatedMember);
    }

    /**
     * 退出群组
     */
    @DeleteMapping("/exit")
    public ResponseEntity<Void> exitGroup(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        groupMemberService.exitGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 移除群组成员
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeGroupMember(@PathVariable Long id, @RequestParam Long removedId) {
        groupMemberService.removeGroupMember(id, removedId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量移除群组成员
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> removeGroupMembers(@RequestBody List<Long> ids, @RequestParam Long removedId) {
        groupMemberService.removeGroupMembers(ids, removedId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 判断用户是否在群组中
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> isUserInGroup(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        boolean isInGroup = groupMemberService.isUserInGroup(groupId, userId);
        return ResponseEntity.ok(isInGroup);
    }

    /**
     * 批量查询群组成员
     */
    @PostMapping("/batch/groups")
    public ResponseEntity<List<GroupMember>> getGroupMembersByGroupIds(@RequestBody List<Long> groupIds) {
        List<GroupMember> members = groupMemberService.getGroupMembersByGroupIds(groupIds);
        return ResponseEntity.ok(members);
    }

    /**
     * 获取群组中的管理员列表
     */
    @GetMapping("/{groupId}/admins")
    public ResponseEntity<List<GroupMember>> getGroupAdmins(@PathVariable Long groupId, @RequestParam String role) {
        List<GroupMember> admins = groupMemberService.getGroupAdmins(groupId, role);
        return ResponseEntity.ok(admins);
    }
}