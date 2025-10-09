package com.funfun.schedule.controller;

import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.service.ScheduleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * ScheduleItemController类，提供RESTful API接口用于ScheduleItem的增删改查操作
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleItemController {

    private final ScheduleItemService scheduleItemService;

    @Autowired
    public ScheduleItemController(ScheduleItemService scheduleItemService) {
        this.scheduleItemService = scheduleItemService;
    }

    /**
     * 创建日程项
     * @param scheduleItem 日程项对象
     * @return 创建的日程项对象和HTTP状态码
     */
    @PostMapping
    public ResponseEntity<ScheduleItem> createScheduleItem(@RequestBody ScheduleItem scheduleItem) {
        ScheduleItem createdItem = scheduleItemService.createScheduleItem(scheduleItem);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    /**
     * 根据ID查询日程项
     * @param id 日程项ID
     * @return 日程项对象和HTTP状态码
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleItem> getScheduleItemById(@PathVariable Integer id) {
        Optional<ScheduleItem> scheduleItem = scheduleItemService.getScheduleItemById(id);
        return scheduleItem.map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * 查询所有日程项
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/list")
    public ResponseEntity<List<ScheduleItem>> getAllScheduleItems() {
        List<ScheduleItem> scheduleItems = scheduleItemService.getAllScheduleItems();
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 更新日程项
     * @param id 日程项ID
     * @param scheduleItem 新的日程项数据
     * @return 更新后的日程项对象和HTTP状态码
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleItem> updateScheduleItem(@PathVariable Integer id, @RequestBody ScheduleItem scheduleItem) {
        try {
            ScheduleItem updatedItem = scheduleItemService.updateScheduleItem(id, scheduleItem);
            return new ResponseEntity<>(updatedItem, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 删除日程项
     * @param id 日程项ID
     * @return HTTP状态码
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduleItem(@PathVariable Integer id) {
        try {
            scheduleItemService.deleteScheduleItem(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 根据groupId和personId查询日程项
     * @param groupId 组ID
     * @param personId 人员ID
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/group-person")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByGroupIdAndPersonId(
            @RequestParam Integer groupId, @RequestParam Integer personId) {
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByGroupIdAndPersonId(groupId, personId);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 根据groupId查询日程项
     * @param groupId 组ID
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByGroupId(@PathVariable Integer groupId) {
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByGroupId(groupId);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 根据personId查询日程项
     * @param personId 人员ID
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/person/{personId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByPersonId(@PathVariable Integer personId) {
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByPersonId(personId);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 根据itemType查询日程项
     * @param itemType 项目类型
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/type/{itemType}")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByItemType(@PathVariable String itemType) {
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByItemType(itemType);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 根据repeatType查询日程项
     * @param repeatType 重复类型
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/repeat-type/{repeatType}")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByRepeatType(@PathVariable String repeatType) {
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByRepeatType(repeatType);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 批量创建日程项
     * @param scheduleItems 日程项列表
     * @return 创建的日程项列表和HTTP状态码
     */
    @PostMapping("/batch")
    public ResponseEntity<List<ScheduleItem>> batchCreateScheduleItems(@RequestBody List<ScheduleItem> scheduleItems) {
        List<ScheduleItem> createdItems = scheduleItemService.batchCreateScheduleItems(scheduleItems);
        return new ResponseEntity<>(createdItems, HttpStatus.CREATED);
    }

    /**
     * 批量删除日程项
     * @param ids 日程项ID列表
     * @return HTTP状态码
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDeleteScheduleItems(@RequestBody List<Integer> ids) {
        try {
            scheduleItemService.batchDeleteScheduleItems(ids);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}