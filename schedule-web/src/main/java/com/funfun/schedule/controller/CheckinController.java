package com.funfun.schedule.controller;

import com.funfun.schedule.anno.RequiredDataPermission;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.CheckinRequest;
import com.funfun.schedule.dto.GetCheckinRequest;
import com.funfun.schedule.dto.QueryScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.dto.schedule.GetScheduleItemRequest;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.CheckinService;
import com.funfun.schedule.service.ScheduleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

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

    /**
     * 当日范围内的 checkin 记录查询；可选 taskId 过滤。
     */
    @PostMapping("/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<List<CheckinRecordDTO>> getCheckInRecords(@RequestBody GetScheduleItemRequest request) {
        Long groupIdLong = Long.valueOf(request.getGroupId());
        Long userIdLong = Long.valueOf(request.getTargetUserId());
        Long taskId = request.getTaskId() != null ? Long.valueOf(request.getTaskId()) : null;
        java.time.LocalDate from = request.getFromDate() != null ? request.getFromDate().toLocalDate() : null;
        java.time.LocalDate to = request.getToDate() != null ? request.getToDate().toLocalDate() : null;
        return CommonResponse.success(checkinService.getRecordList(groupIdLong, userIdLong, taskId, from, to));
    }

    /**
     * 按 taskKey 列表查询：直接按 task_key 列匹配。
     */
    @PostMapping("/listV2")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<List<CheckinRecordDTO>> getCheckInRecordV2(@RequestBody GetCheckinRequest request) {
        Long groupIdLong = Long.valueOf(request.getGroupId());
        Long userIdLong = Long.valueOf(request.getTargetUserId());
        java.util.Set<String> keys = request.getTaskKeys() == null ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(request.getTaskKeys());
        return CommonResponse.success(checkinService.getRecordList(groupIdLong, userIdLong, keys));
    }

    /**
     * 按 taskIds / parentIds 直接拉对应 ScheduleItem 列表（不按日期分组）。
     */
    @PostMapping("/task/listV2")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<?> getTaskList(@RequestBody GetScheduleItemRequest request) {
        if (request.getTaskIds() != null && !request.getTaskIds().isEmpty()) {
            List<Long> ids = request.getTaskIds().stream().map(Long::valueOf).collect(Collectors.toList());
            return CommonResponse.success(scheduleItemService.getItemList(ids));
        }
        if (request.getParentIds() != null && !request.getParentIds().isEmpty()) {
            List<Long> ids = request.getParentIds().stream().map(Long::valueOf).collect(Collectors.toList());
            return CommonResponse.success(scheduleItemService.getItemListByParentIds(ids));
        }
        CommonException.DATA_INVALID.throwsError();
        return null;
    }

    /**
     * 任务列表查询：支持
     *   - 按日期范围（fromDate/toDate + scheduleItemType）
     *   - 按 taskIds
     *   - 按 parentIds
     */
    @PostMapping("/task/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<?> getTasks(@RequestBody GetScheduleItemRequest request) throws ParseException {
        if (request.getGroupId() == null && request.getTargetUserId() == null
                && (request.getTaskIds() == null || request.getTaskIds().isEmpty())
                && (request.getParentIds() == null || request.getParentIds().isEmpty())) {
            CommonException.DATA_INVALID.throwsError("groupId/userId or taskIds/parentIds are required");
        }
        Long groupIdLong = request.getGroupId() == null ? null : Long.valueOf(request.getGroupId());
        Long userIdLong = request.getTargetUserId() == null ? null : Long.valueOf(request.getTargetUserId());

        QueryScheduleItemDTO query = new QueryScheduleItemDTO();
        query.setFromDate(request.getFromDate());
        query.setToDate(request.getToDate());
        // task/list 入口语义就是 task；维持原版硬编码
        query.setScheduleItemType(com.funfun.schedule.enums.ScheduleItemType.task);
        if (request.getTaskIds() != null && !request.getTaskIds().isEmpty()) {
            query.setTaskIds(request.getTaskIds().stream().map(Long::valueOf).collect(Collectors.toList()));
        } else if (request.getParentIds() != null && !request.getParentIds().isEmpty()) {
            query.setParentIds(request.getParentIds().stream().map(Long::valueOf).collect(Collectors.toList()));
        }

        List<ScheduleListItemDTO> scheduleItemsByDate =
                scheduleItemService.getScheduleItemsByDateRange(groupIdLong, userIdLong, query);
        return CommonResponse.success(scheduleItemsByDate);
    }
}
