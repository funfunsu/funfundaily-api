package com.funfun.schedule.controller;

import com.funfun.schedule.entity.ScheduleGroup;
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
    public ResponseEntity<ScheduleGroup> createGroup(@RequestBody ScheduleGroup scheduleGroup) {
        ScheduleGroup createdGroup = scheduleGroupService.createGroup(scheduleGroup);
        return new ResponseEntity<>(createdGroup, HttpStatus.CREATED);
    }

    /**
     * 根据ID查询群组
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleGroup> getGroupById(@PathVariable Long id) {
        ScheduleGroup group = scheduleGroupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    /**
     * 查询所有群组
     */
    @GetMapping
    public ResponseEntity<List<ScheduleGroup>> getAllGroups() {
        List<ScheduleGroup> groups = scheduleGroupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据创建者ID查询群组
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<ScheduleGroup>> getGroupsByCreator(@PathVariable Long creatorId) {
        List<ScheduleGroup> groups = scheduleGroupService.getGroupsByCreator(creatorId);
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据标题模糊查询群组
     */
    @GetMapping("/search")
    public ResponseEntity<List<ScheduleGroup>> getGroupsByTitleContaining(@RequestParam String title) {
        List<ScheduleGroup> groups = scheduleGroupService.getGroupsByTitleContaining(title);
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据创建时间范围查询群组
     */
    @GetMapping("/create-time")
    public ResponseEntity<List<ScheduleGroup>> getGroupsByCreateTimeBetween(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<ScheduleGroup> groups = scheduleGroupService.getGroupsByCreateTimeBetween(startDate, endDate);
        return ResponseEntity.ok(groups);
    }

    /**
     * 更新群组信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleGroup> updateGroup(@PathVariable Long id, @RequestBody ScheduleGroup scheduleGroup) {
        scheduleGroup.setId(id);
        ScheduleGroup updatedGroup = scheduleGroupService.updateGroup(scheduleGroup);
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
    public ResponseEntity<List<ScheduleGroup>> getGroupsByIds(@RequestBody List<Long> ids) {
        List<ScheduleGroup> groups = scheduleGroupService.getGroupsByIds(ids);
        return ResponseEntity.ok(groups);
    }

    /**
     * 根据标题和创建者查询群组
     */
    @GetMapping("/title-creator")
    public ResponseEntity<ScheduleGroup> getGroupByTitleAndCreator(
            @RequestParam String title,
            @RequestParam Long creator) {
        ScheduleGroup group = scheduleGroupService.getGroupByTitleAndCreator(title, creator);
        return ResponseEntity.ok(group);
    }
}