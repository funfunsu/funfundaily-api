package com.funfun.schedule.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.funfun.schedule.anno.RequiredDataPermission;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.dto.schedule.CopyScheduleItemRequest;
import com.funfun.schedule.dto.schedule.CreateScheduleItemRequest;
import com.funfun.schedule.dto.schedule.AScheduleItemRequest;
import com.funfun.schedule.dto.schedule.CloseScheduleItemRequest;
import com.funfun.schedule.dto.schedule.GetScheduleItemRequest;
import com.funfun.schedule.entity.ShareRecord;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.ScheduleGroupService;
import com.funfun.schedule.service.ScheduleItemService;
import com.funfun.schedule.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * ScheduleItemController类，提供RESTful API接口用于ScheduleItem的增删改查操作
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleItemController {



    private final ScheduleItemService scheduleItemService;
    private final ShareService shareService;

    @Autowired
    public ScheduleItemController(ScheduleItemService scheduleItemService, ScheduleGroupService scheduleGroupService, ShareService shareService) {
        this.scheduleItemService = scheduleItemService;
        this.shareService = shareService;
    }

    /**
     * 创建日程项
     * @return 创建的日程项对象和HTTP状态码
     */
    @PostMapping("/save")
    @RequiredDataPermission
    public CommonResponse<Boolean> createScheduleItem(@RequestBody CreateScheduleItemRequest request) {
        Long userId = UserContext.getUserId();
        Long groupId = Long.valueOf(request.getGroupId());
        Boolean success = scheduleItemService.createScheduleItems(userId, groupId, Long.valueOf(request.getTargetUserId()),request.getItems());
        return CommonResponse.success(success);
    }
    /**
     * 创建日程项
     * @return 创建的日程项对象和HTTP状态码
     */
    @PostMapping("/copy")
    @RequiredDataPermission
    public CommonResponse<Boolean> createScheduleItem(@RequestBody CopyScheduleItemRequest request) {
        Optional<ShareRecord> shareRecordOptional = shareService.getShareByToken(request.getShareToken());
        if (!shareRecordOptional.isPresent()){
            CommonException.DATA_INVALID.throwsError("分享内容不存在");
        }
        String shareContent = shareRecordOptional.get().getContent();
        List<ScheduleItemDTO> list = JSON.parseObject(shareContent,new TypeReference<List<ScheduleItemDTO>>(){});
        list.forEach(scheduleItemDTO -> {scheduleItemDTO.setId(null);});
        Long userId = UserContext.getUserId();
        Long groupId = Long.valueOf(request.getGroupId());
        Boolean success = scheduleItemService.createScheduleItems(userId, groupId, Long.valueOf(request.getTargetUserId()),list);
        return CommonResponse.success(success);
    }
    /**
     * 根据ID查询日程项
     * @param id 日程项ID
     * @return 日程项对象和HTTP状态码
     */
    @GetMapping("/{id}")
    public CommonResponse<ScheduleItemDTO> getScheduleItemById(@PathVariable String id) {
        Long idLong = Long.parseLong(id);
        ScheduleItemDTO scheduleItem = scheduleItemService.getScheduleItemById(idLong);
        return CommonResponse.success(scheduleItem);
    }
    /**
     * 根据ID查询日程项
     * @return 日程项对象和HTTP状态码
     */
    @PostMapping("/get")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<ScheduleItemDTO> getScheduleItem(@RequestBody AScheduleItemRequest request) {
        Long idLong = Long.parseLong(request.getId());
        ScheduleItemDTO scheduleItem = scheduleItemService.getScheduleItemById(idLong);
        return CommonResponse.success(scheduleItem);
    }

    /**
     * 查询所有日程项
     * @return 日程项列表和HTTP状态码
     */
    @PostMapping("/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin,GroupRole.Member})
    public CommonResponse<?> getScheduleItems(@RequestBody GetScheduleItemRequest request) {
        Long groupIdLong = request.getGroupId() == null? null : Long.valueOf(request.getGroupId());
        Long userIdLong = request.getTargetUserId() == null? null: Long.valueOf(request.getTargetUserId());

        if (groupIdLong == null && userIdLong == null){
            //默认是查自己
            userIdLong = UserContext.getUserId();
        }
        List<ScheduleListItemDTO> scheduleItemsByDate =
                scheduleItemService.getScheduleItemsByDateRange(groupIdLong, userIdLong, request.getFromDate(), request.getToDate(), request.getScheduleItemType());
        return CommonResponse.success(scheduleItemsByDate);
    }

    /**
     * 停止关注 / 恢复关注 某个日程项（事件）。
     * closeStatus = CLOSE 停止关注（全局隐藏），OPEN 恢复关注。
     */
    @PostMapping("/close")
    @RequiredDataPermission
    public CommonResponse<Boolean> close(@RequestBody CloseScheduleItemRequest request) {
        Long idLong = Long.parseLong(request.getId());
        CloseStatus closeStatus = request.getCloseStatus() == null ? CloseStatus.CLOSE : request.getCloseStatus();
        scheduleItemService.updateCloseStatus(idLong, closeStatus);
        return CommonResponse.success(true);
    }

    /**
     * 月度计划：返回某群组下、指定类型（monthlyPlan）的全部未关闭项（原始列表，不按天展开）。
     * 家庭月度计划按群组共享，前端据此按月份归属一次性 / 周期性事件。
     */
    @PostMapping("/plan/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<?> planList(@RequestBody GetScheduleItemRequest request) {
        Long groupIdLong = Long.valueOf(request.getGroupId());
        ScheduleItemType type = request.getScheduleItemType() == null
                ? ScheduleItemType.monthlyPlan : request.getScheduleItemType();
        return CommonResponse.success(scheduleItemService.getPlanItems(groupIdLong, type));
    }

    /**
     * 查询已停止关注（CLOSE）的日程项列表，用于「恢复关注」入口。
     */
    @PostMapping("/closed/list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<?> closedList(@RequestBody GetScheduleItemRequest request) {
        Long groupIdLong = request.getGroupId() == null ? null : Long.valueOf(request.getGroupId());
        Long userIdLong = request.getTargetUserId() == null ? UserContext.getUserId() : Long.valueOf(request.getTargetUserId());
        return CommonResponse.success(scheduleItemService.getClosedItems(groupIdLong, userIdLong, request.getScheduleItemType()));
    }

    /**
     * 删除日程项
     * @param id 日程项ID
     * @return HTTP状态码
     */
    @DeleteMapping("/{id}")
    public CommonResponse<Void> deleteScheduleItem(@PathVariable String id) {
        Long idLong = Long.parseLong(id);
        scheduleItemService.deleteScheduleItem(idLong);
        return CommonResponse.success();
    }
    /**
     * 删除日程项
     * @return HTTP状态码
     */
    @PostMapping("/delete")
    @RequiredDataPermission
    public CommonResponse<Void> delete(@RequestBody AScheduleItemRequest request) {
        Long idLong = Long.parseLong(request.getId());
        scheduleItemService.deleteScheduleItem(idLong);
        return CommonResponse.success();
    }
}