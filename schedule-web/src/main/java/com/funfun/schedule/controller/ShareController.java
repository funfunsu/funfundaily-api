package com.funfun.schedule.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.InviteMemberShareDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.ShareRecord;
import com.funfun.schedule.entity.User;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareService shareService;
    @Autowired
    private UserService userService;

    @Autowired
    private ScheduleItemService scheduleItemService;

    @Autowired
    private GroupMemberService groupMemberService;
    @Autowired
    private ScheduleGroupService scheduleGroupService;
    @Autowired
    private WeChatQrcodeService weChatQrcodeService;

    /** 识别二维码后默认打开的小程序页面（不含前导斜杠、不带 query） */
    private static final String DEFAULT_QRCODE_PAGE = "pages/task/share";

    // 创建分享
    @PostMapping("/create")
    public CommonResponse<Map<String, String>> createShare(@RequestBody CreateShareRequest request) {
        String token = shareService.createShare(
                UserContext.getUserId(),
                request.getSceneCode(),
                request.getContent(),
                request.getExpireHours()
        );
        return CommonResponse.success(Map.of("token", token));
    }

    /**
     * 生成分享二维码（微信无限制小程序码）。
     * 以分享 token 作为 scene，识别后打开 page 并在 query.scene 中带回 token。
     * 返回 base64，前端写临时文件后画进分享长图。
     */
    @PostMapping("/qrcode")
    public CommonResponse<Map<String, String>> shareQrcode(@RequestBody ShareQrcodeRequest request) {
        String page = request.getPage() != null && !request.getPage().isBlank()
                ? request.getPage() : DEFAULT_QRCODE_PAGE;
        byte[] png = weChatQrcodeService.getUnlimitedQrCode(request.getToken(), page);
        Map<String, String> result = new HashMap<>();
        result.put("contentType", "image/png");
        result.put("qrBase64", Base64.getEncoder().encodeToString(png));
        return CommonResponse.success(result);
    }

    // 获取分享内容
    @GetMapping("/{token}")
    public CommonResponse<?> getShare(@PathVariable String token) throws ParseException {
        Optional<ShareRecord> record = shareService.getShareByToken(token);
        if (record.isEmpty()) {
            return CommonResponse.success();
        }
        ShareRecord shareRecord = record.get();
        User user = userService.getUserById(shareRecord.getCreatorId());

        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("creatorNickname",user.getNickname());

        switch (shareRecord.getSceneCode()){
            case "schedule_share":
                // 1. 获取当前日期
                LocalDate today = LocalDate.now();
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                String fromDate = monday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate sunday = today.with(DayOfWeek.SUNDAY);
                String toDate = sunday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                List<ScheduleItemDTO> list = JSON.parseObject(shareRecord.getContent(),new TypeReference<List<ScheduleItemDTO>>(){});
                return CommonResponse.success(scheduleItemService.transferToDateScheduleItems(ScheduleItemType.schedule,fromDate,toDate,list));
            case "member_share":
                JSONObject shareContent = JSON.parseObject(shareRecord.getContent());
                infoMap.put("data",shareContent);
                return CommonResponse.success(infoMap);
            case "invitation":
                JSONObject invitationContent = JSON.parseObject(shareRecord.getContent());
                infoMap.put("data", invitationContent);
                return CommonResponse.success(infoMap);
            case "task_share":
                // 任务分享：content 是选中任务的 JSON 数组，返回 { creatorNickname, data:[...] }
                JSONArray taskList = JSON.parseArray(shareRecord.getContent());
                infoMap.put("data", taskList);
                return CommonResponse.success(infoMap);

        }
        return  CommonResponse.success(record.get().getContent());
    }
    // 获取分享内容
    @PostMapping("/accept/{token}")
    public CommonResponse<?> acceptShare(@PathVariable String token) throws ParseException {
        Optional<ShareRecord> record = shareService.getShareByToken(token);
        if (record.isEmpty()) {
            return CommonResponse.success();
        }
        ShareRecord shareRecord = record.get();
        switch (shareRecord.getSceneCode()){
            case "member_share":
                InviteMemberShareDTO inviteMemberShareDTO = JSON.parseObject(shareRecord.getContent(), InviteMemberShareDTO.class);
                String groupIdStr = inviteMemberShareDTO.getGroupId();
                GroupMember groupMember = new GroupMember();
                groupMember.setGroupId(Long.valueOf(groupIdStr));
                groupMember.setInviterId(shareRecord.getCreatorId());
                groupMember.setUserId(UserContext.getUserId());
                groupMember.setRole(inviteMemberShareDTO.getRole());
                groupMemberService.joinGroup(groupMember);

        }
        return  CommonResponse.success(record.get().getContent());
    }

    // 请求体 DTO
    public static class CreateShareRequest {
        private String content;
        private String sceneCode;

        public String getSceneCode() {
            return sceneCode;
        }

        public void setSceneCode(String sceneCode) {
            this.sceneCode = sceneCode;
        }

        private int expireHours = 24; // 默认24小时
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getExpireHours() { return expireHours; }
        public void setExpireHours(int expireHours) { this.expireHours = expireHours; }
    }

    // 生成分享二维码请求体
    public static class ShareQrcodeRequest {
        private String token;   // 分享 token（作为小程序码 scene）
        private String page;    // 可选，识别后打开的小程序页面，默认 pages/task/share
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPage() { return page; }
        public void setPage(String page) { this.page = page; }
    }
}