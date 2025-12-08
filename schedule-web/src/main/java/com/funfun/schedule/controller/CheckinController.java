package com.funfun.schedule.controller;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.CheckinRequest;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.CheckinService;
import com.funfun.schedule.service.ScheduleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {
    // 定义你期望的日期时间格式
    private static final DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private ScheduleItemService scheduleItemService;

    @PostMapping("/task/complete")
    public CommonResponse<?> checkIn(@RequestBody CheckinRequest requestDto) {
        CheckinRecordDTO checkinRecordDTO = new CheckinRecordDTO();
        checkinRecordDTO.setGroupId(Long.valueOf(requestDto.getGroupId()));
        checkinRecordDTO.setUserId(Long.valueOf(requestDto.getUserId()));
        checkinRecordDTO.setExtra(requestDto.getExtraInfo());
        checkinRecordDTO.setTaskId(Long.valueOf(requestDto.getTaskId()));
        checkinRecordDTO.setOperatorId(UserContext.getUserId());

        Long recordId = checkinService.performCheckin(checkinRecordDTO);
        return CommonResponse.success(recordId);
    }
    @GetMapping("/list")
    public CommonResponse<?> listCheckIn(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        LocalDate localDate = LocalDate.parse(fromDate, CUSTOM_FORMATTER);
        LocalDateTime from = localDate.atStartOfDay();
        LocalDateTime to = LocalDate.parse(toDate, CUSTOM_FORMATTER).atStartOfDay();
        Long groupIdLong = Long.valueOf(groupId);
        Long userIdLong = Long.valueOf(userId);
        return CommonResponse.success(checkinService.getRecordList(groupIdLong,userIdLong,from,to));
    }
    @GetMapping("/task/list")
    public CommonResponse<?> listTask(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        Long groupIdLong = Long.valueOf(groupId);
        Long userIdLong = Long.valueOf(userId);
        return CommonResponse.success(scheduleItemService.getTaskItemsByDateRange(groupIdLong,userIdLong,fromDate,toDate));
    }

}