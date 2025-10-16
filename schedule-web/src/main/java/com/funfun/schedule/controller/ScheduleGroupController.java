package com.funfun.schedule.controller;

import com.funfun.schedule.entity.Group;
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
@RequestMapping("/api/groups")
public class ScheduleGroupController {

    @Autowired
    private ScheduleGroupService scheduleGroupService;

    /**
     * 创建群组
     */
    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        Group createdGroup = scheduleGroupService.createGroup(group);
        return new ResponseEntity<>(createdGroup, HttpStatus.CREATED);
    }

    /**
     * 根据ID查询群组
     */
    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable Long id) {
        Group group = scheduleGroupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    /**
     * 查询所有群组
     */
    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        List<Group> groups = scheduleGroupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据创建者ID查询群组
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<Group>> getGroupsByCreator(@PathVariable Long creatorId) {
        List<Group> groups = scheduleGroupService.getGroupsByCreator(creatorId);
        return ResponseEntity.ok(groups);
    }
    
    /**
     * 根据用户ID查询该用户所在的所有群组
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Group>> getGroupsByUserId(@PathVariable Long userId) {
        List<Group> groups = scheduleGroupService.getGroupsByUserId(userId);
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据标题模糊查询群组
     */
    @GetMapping("/search")
    public ResponseEntity<List<Group>> getGroupsByTitleContaining(@RequestParam String title) {
        List<Group> groups = scheduleGroupService.getGroupsByTitleContaining(title);
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据创建时间范围查询群组
     */
    @GetMapping("/create-time")
    public ResponseEntity<List<Group>> getGroupsByCreateTimeBetween(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Group> groups = scheduleGroupService.getGroupsByCreateTimeBetween(startDate, endDate);
        return ResponseEntity.ok(groups);
    }

    /**
     * 更新群组信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Group> updateGroup(@PathVariable Long id, @RequestBody Group group) {
        group.setId(id);
        Group updatedGroup = scheduleGroupService.updateGroup(group);
        return ResponseEntity.ok(updatedGroup);
    }

    /**
     * 删除群组
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        scheduleGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量删除群组
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteGroups(@RequestBody List<Long> ids) {
        scheduleGroupService.deleteGroups(ids);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量查询群组
     */
    @PostMapping("/batch")
    public ResponseEntity<List<Group>> getGroupsByIds(@RequestBody List<Long> ids) {
        List<Group> groups = scheduleGroupService.getGroupsByIds(ids);
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据标题和创建者查询群组
     */
    @GetMapping("/title-creator")
    public ResponseEntity<Group> getGroupByTitleAndCreator(
            @RequestParam String title,
            @RequestParam Long creator) {
        Group group = scheduleGroupService.getGroupByTitleAndCreator(title, creator);
        return ResponseEntity.ok(group);
    }
}