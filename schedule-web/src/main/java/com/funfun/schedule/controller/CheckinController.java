package com.funfun.schedule.controller;

import com.funfun.schedule.anno.RequiredDataPermission;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.CheckinRequest;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.dto.schedule.GetScheduleItemRequest;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.CheckinService;
import com.funfun.schedule.service.ScheduleItemService;
import com.funfun.schedule.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    @RequiredDataPermission
    public CommonResponse<?> checkIn(@RequestBody CheckinRequest requestDto) {
        if (requestDto.getTaskTime()!= null && requestDto.getTaskTime().isAfter(LocalDateTime.now())){
            CommonException.NOT_ALLOWED.throwsError("还没到打卡时间");
        }

        CheckinRecordDTO checkinRecordDTO = new CheckinRecordDTO();
        checkinRecordDTO.setGroupId(Long.valueOf(requestDto.getGroupId()));
        checkinRecordDTO.setUserId(Long.valueOf(requestDto.getTargetUserId()));
        checkinRecordDTO.setExtra(requestDto.getExtra());
        checkinRecordDTO.setTaskId(Long.valueOf(requestDto.getTaskId()));
        checkinRecordDTO.setOperatorId(UserContext.getUserId());
        checkinRecordDTO.setTaskTime(requestDto.getTaskTime());

        Long recordId = checkinService.performCheckin(checkinRecordDTO);
        return CommonResponse.success(recordId);
    }
    @GetMapping("/list")
    @Deprecated
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
    @PostMapping("/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getCheckInRecords(GetScheduleItemRequest request) {
        LocalDateTime from = DateUtil.getStartOfDay(request.getFromDate());
        LocalDateTime to = DateUtil.getEndOfDay(request.getFromDate());
        Long groupIdLong = Long.valueOf(request.getGroupId());
        Long userIdLong = Long.valueOf(request.getTargetUserId());
        return CommonResponse.success(checkinService.getRecordList(groupIdLong,userIdLong,from,to));
    }

    @GetMapping("/task/list")
    @Deprecated
    public CommonResponse<?> listTask(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String fromDate) {
        Long groupIdLong = Long.valueOf(groupId);
        Long userIdLong = Long.valueOf(userId);
        List<ScheduleListItemDTO>  list = scheduleItemService.getTaskItemsByDateRange(groupIdLong,userIdLong,fromDate,fromDate);
        return CommonResponse.success(list.get(0).getSchedules());
    }


    /**
     * 查询所有日程项
     * @return 日程项列表和HTTP状态码
     */
    @PostMapping("/task/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getTasks(GetScheduleItemRequest request) {
        // 检查必要参数
        if (request.getGroupId() == null &&  request.getTargetUserId() == null) {
            CommonException.DATA_INVALID.throwsError("groupId and userId are required");
        }
        Long groupIdLong = request.getGroupId() == null? null : Long.valueOf(request.getGroupId());
        Long userIdLong = request.getGroupId() == null? null : Long.valueOf(request.getTargetUserId());

        List<ScheduleListItemDTO> scheduleItemsByDate =
                scheduleItemService.getScheduleItemsByDateRange(groupIdLong, userIdLong, request.getFromDate(), request.getToDate(), ScheduleItemType.task);
        return CommonResponse.success(scheduleItemsByDate);
    }

}