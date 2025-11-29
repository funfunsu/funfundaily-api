package com.funfun.schedule.controller;

import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.dto.schedule.CreateScheduleItemRequest;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.ScheduleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
     * @return 创建的日程项对象和HTTP状态码
     */
    @PostMapping("/add")
    public ResponseEntity<ScheduleItem> createScheduleItem(@RequestBody CreateScheduleItemRequest request) {
        Long userId = Long.valueOf(request.getUserId());
        Long groupId = Long.valueOf(request.groupId);
        ScheduleItem createdItem = scheduleItemService.createScheduleItems(userId, groupId, request.getItems());
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    /**
     * 根据ID查询日程项
     * @param id 日程项ID
     * @return 日程项对象和HTTP状态码
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleItemDTO> getScheduleItemById(@PathVariable String id) {
        Long idLong = Long.parseLong(id);
        ScheduleItemDTO scheduleItem = scheduleItemService.getScheduleItemById(idLong);
        return new ResponseEntity<>(scheduleItem, HttpStatus.OK);
    }

    /**
     * 查询所有日程项
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/list")
    public CommonResponse<?> getAllScheduleItems(
            @RequestParam(required = false) String groupId, 
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        // 检查必要参数
        if (groupId == null || userId == null) {
            CommonException.DATA_INVALID.throwsError("groupId and userId are required");
        }
        
        Long groupIdLong = Long.parseLong(groupId);
        Long userIdLong = Long.parseLong(userId);

        // 如果提供了fromDate和toDate，则按日期范围查询并分组
        if (fromDate == null || toDate == null) {
            LocalDateTime localDateTime = LocalDateTime.now();
            fromDate = new SimpleDateFormat("yyyy-MM-dd").format(localDateTime);
            toDate = new SimpleDateFormat("yyyy-MM-dd").format(Date.from(localDateTime.plusDays(7).atZone(java.time.ZoneId.systemDefault()).toInstant()));

        }
        List<ScheduleListItemDTO> scheduleItemsByDate =
                scheduleItemService.getScheduleItemsByDateRange(groupIdLong, userIdLong, fromDate, toDate);
        return CommonResponse.success(scheduleItemsByDate);
    }

    /**
     * 更新日程项
     * @param id 日程项ID
     * @param scheduleItem 新的日程项数据
     * @return 更新后的日程项对象和HTTP状态码
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleItem> updateScheduleItem(@PathVariable String id, @RequestBody ScheduleItem scheduleItem) {
        try {
            Long idLong = Long.parseLong(id);
            ScheduleItem updatedItem = scheduleItemService.updateScheduleItem(idLong, scheduleItem);
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
    public ResponseEntity<Void> deleteScheduleItem(@PathVariable String id) {
        try {
            Long idLong = Long.parseLong(id);
            scheduleItemService.deleteScheduleItem(idLong);
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
            @RequestParam String groupId, @RequestParam String personId) {
        Long groupIdLong = Long.parseLong(groupId);
        Long personIdLong = Long.parseLong(personId);
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByGroupIdAndPersonId(groupIdLong, personIdLong);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 根据groupId查询日程项
     * @param groupId 组ID
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByGroupId(@PathVariable String groupId) {
        Long groupIdLong = Long.parseLong(groupId);
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByGroupId(groupIdLong);
        return new ResponseEntity<>(scheduleItems, HttpStatus.OK);
    }

    /**
     * 根据personId查询日程项
     * @param personId 人员ID
     * @return 日程项列表和HTTP状态码
     */
    @GetMapping("/person/{personId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleItemsByPersonId(@PathVariable String personId) {
        Long personIdLong = Long.valueOf(personId);
        List<ScheduleItem> scheduleItems = scheduleItemService.getScheduleItemsByPersonId(personIdLong);
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
     * 批量删除日程项
     * @param ids 日程项ID列表
     * @return HTTP状态码
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDeleteScheduleItems(@RequestBody List<String> ids) {
        try {
            List<Long> idLongs = ids.stream().map(Long::parseLong).collect(Collectors.toList());
            scheduleItemService.batchDeleteScheduleItems(idLongs);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}