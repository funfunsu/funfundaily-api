package com.funfun.schedule.controller;

import com.funfun.schedule.anno.RequiredDataPermission;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.*;
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

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    @PostMapping("/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getCheckInRecords(@RequestBody GetScheduleItemRequest request) {
        Long groupIdLong = Long.valueOf(request.getGroupId());
        Long userIdLong = Long.valueOf(request.getTargetUserId());
        Long taskId = null;
        if (request.getTaskId() != null){
            taskId = Long.valueOf(request.getTaskId());
        }
        return CommonResponse.success(checkinService.getRecordList(groupIdLong,userIdLong,taskId,request.getFromDate(),request.getToDate()));
    }
    @PostMapping("/listV2")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getCheckInRecordV2(@RequestBody GetCheckinRequest request) {
        Long groupIdLong = Long.valueOf(request.getGroupId());
        Long userIdLong = Long.valueOf(request.getTargetUserId());
        return CommonResponse.success(checkinService.getRecordList(groupIdLong,userIdLong,request.getTaskKeys()));
    }



    /**
     * 查询所有日程项
     * @return 日程项列表和HTTP状态码
     */
    @PostMapping("/task/listV2")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getTaskList(@RequestBody GetScheduleItemRequest request) throws ParseException {
        List<Long> taskIds = null;
        if (request.getTaskIds()!= null && !request.getTaskIds().isEmpty()){
            taskIds = request.getTaskIds().stream().map(Long::valueOf).collect(Collectors.toList());
            return CommonResponse.success(scheduleItemService.getItemList(taskIds));

        }else if (request.getParentIds()!= null && !request.getParentIds().isEmpty()){
            taskIds = request.getParentIds().stream().map(Long::valueOf).collect(Collectors.toList());
            return CommonResponse.success(scheduleItemService.getItemListByParentIds(taskIds));
        }
        CommonException.DATA_INVALID.throwsError();
        return null;
    }


    /**
     * 查询所有日程项
     * @return 日程项列表和HTTP状态码
     */
    @PostMapping("/task/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getTasks(@RequestBody GetScheduleItemRequest request) throws ParseException {
        // 检查必要参数
        if (request.getGroupId() == null &&  request.getTargetUserId() == null && request.getTaskIds() == null) {
            CommonException.DATA_INVALID.throwsError("groupId and userId are required");
        }
        Long groupIdLong = request.getGroupId() == null? null : Long.valueOf(request.getGroupId());
        Long userIdLong = request.getGroupId() == null? null : Long.valueOf(request.getTargetUserId());

        QueryScheduleItemDTO queryScheduleItemDTO = new QueryScheduleItemDTO();
        queryScheduleItemDTO.setFromDate(request.getFromDate());
        queryScheduleItemDTO.setToDate(request.getToDate());
        queryScheduleItemDTO.setScheduleItemType(request.getScheduleItemType());
        List<Long> taskIds = null;
        if (request.getTaskIds()!= null && !request.getTaskIds().isEmpty()){
            taskIds = request.getTaskIds().stream().map(Long::valueOf).collect(Collectors.toList());
            queryScheduleItemDTO.setTaskIds(taskIds);

        }else if (request.getParentIds()!= null && !request.getParentIds().isEmpty()){
            taskIds = request.getParentIds().stream().map(Long::valueOf).collect(Collectors.toList());
            queryScheduleItemDTO.setParentIds(taskIds);
        }
        List<ScheduleListItemDTO> scheduleItemsByDate =
                scheduleItemService.getScheduleItemsByDateRange(groupIdLong, userIdLong, queryScheduleItemDTO);
        return CommonResponse.success(scheduleItemsByDate);
    }

}