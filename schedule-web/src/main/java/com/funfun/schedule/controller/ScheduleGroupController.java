package com.funfun.schedule.controller;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.GroupDTO;
import com.funfun.schedule.entity.Group;
import com.funfun.schedule.enums.GroupType;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.ScheduleGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * ScheduleGroupController类，提供群组相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/group")
public class ScheduleGroupController {

    @Autowired
    private ScheduleGroupService scheduleGroupService;

    /**
     *  curl -X POST http://localhost:8080/api/groups/create \
     *   -H "Content-Type: application/json" \
     *   -d '{"groupName": "VVvv"}'
     * 创建群组
     */
    @PostMapping("create")
    public CommonResponse<Group> createGroup(@RequestBody GroupDTO group) {
        group.setType(GroupType.Manual.ordinal());
        Group createdGroup = scheduleGroupService.createGroup(group);
        return CommonResponse.success(createdGroup);
    }
    @PostMapping("modify")
    public CommonResponse<Group> modifyGroup(@RequestBody GroupDTO group) {
        Group createdGroup = scheduleGroupService.updateGroup(group);
        return CommonResponse.success(createdGroup);
    }

    /**
     * 根据ID查询群组
     */
    @GetMapping("/{id}")
    public CommonResponse<Group> getGroupById(@PathVariable Long id) {
        Group group = scheduleGroupService.getGroupById(id);
        return CommonResponse.success(group);
    }

    /**
     * 查询所有群组
     */
    @GetMapping("/list")
    public CommonResponse getGroups() {
        Long userId =  UserContext.getUserId();
        List<Group> groups = scheduleGroupService.getGroupList(userId);
        return CommonResponse.success(groups);
    }

    /**
     * 根据创建者ID查询群组
     */
    @GetMapping("/creator/{creatorId}")
    public CommonResponse getGroupsByCreator(@PathVariable Long creatorId) {
        List<Group> groups = scheduleGroupService.getGroupsByCreator(creatorId);
        return CommonResponse.success(groups);
    }

    /**
     * 根据标题模糊查询群组
     */
    @GetMapping("/search")
    public CommonResponse<List<Group>> getGroupsByTitleContaining(@RequestParam String title) {
        List<Group> groups = scheduleGroupService.getGroupsByTitleContaining(title);
        return CommonResponse.success(groups);
    }

    /**
     * 根据创建时间范围查询群组
     */
    @GetMapping("/create-time")
    public CommonResponse getGroupsByCreateTimeBetween(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Group> groups = scheduleGroupService.getGroupsByCreateTimeBetween(startDate, endDate);
        return CommonResponse.success(groups);
    }

    /**
     * 更新群组信息
     */
    @PutMapping("/{id}")
    public CommonResponse<Group> updateGroup(@PathVariable Long id, @RequestBody GroupDTO group) {
        group.setId(id);
        Group updatedGroup = scheduleGroupService.updateGroup(group);
        return CommonResponse.success(updatedGroup);
    }

    /**
     * 删除群组
     */
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteGroup(@PathVariable Long id) {
        scheduleGroupService.deleteGroup(id);
        return CommonResponse.success();
    }

    /**
     * 批量删除群组
     */
    @DeleteMapping("/batch")
    public CommonResponse deleteGroups(@RequestBody List<Long> ids) {
        scheduleGroupService.deleteGroups(ids);
        return CommonResponse.success();
    }

    /**
     * 批量查询群组
     */
    @PostMapping("/batch")
    public CommonResponse getGroupsByIds(@RequestBody List<Long> ids) {
        List<Group> groups = scheduleGroupService.getGroupsByIds(ids);
        return CommonResponse.success(groups);
    }

    /**
     * 根据标题和创建者查询群组
     */
    @GetMapping("/title-creator")
    public CommonResponse getGroupByTitleAndCreator(
            @RequestParam String title,
            @RequestParam Long creator) {
        Group group = scheduleGroupService.getGroupByTitleAndCreator(title, creator);
        return CommonResponse.success(group);
    }
}