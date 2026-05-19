package com.funfun.schedule.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.entity.UniversalRecord;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.repository.UniversalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitation")
public class InvitationController {

    private static final String SCENE = "invitation";

    @Autowired
    private UniversalRecordRepository universalRecordRepository;

    @GetMapping("/list")
    public CommonResponse<List<InvitationDTO>> list(@RequestParam Long groupId) {
        if (groupId == null) {
            CommonException.PARAM_INVALID.throwsError("groupId 不能为空");
        }
        List<UniversalRecord> records =
                universalRecordRepository.findBySceneAndSceneVarOrderByCreatedTimeDesc(SCENE, String.valueOf(groupId));
        List<InvitationDTO> result = new ArrayList<>();
        for (UniversalRecord r : records) {
            result.add(toDTO(r));
        }
        return CommonResponse.success(result);
    }

    @PostMapping("/save")
    public CommonResponse<InvitationDTO> save(@RequestBody SaveInvitationRequest request) {
        Long userId = UserContext.getUserId();
        if (request.getGroupId() == null) {
            CommonException.PARAM_INVALID.throwsError("groupId 不能为空");
        }

        JSONObject content = new JSONObject();
        content.put("title", request.getTitle());
        content.put("eventTime", request.getEventTime());
        content.put("address", request.getAddress());
        content.put("body", request.getBody());
        content.put("cardStyle", request.getCardStyle());

        UniversalRecord record;
        if (request.getId() != null) {
            Optional<UniversalRecord> existing = universalRecordRepository.findById(request.getId());
            if (existing.isEmpty() || !userId.equals(existing.get().getCreatedBy())) {
                CommonException.NOT_ALLOWED.throwsError("邀请函不存在或无权限");
            }
            record = existing.get();
        } else {
            record = new UniversalRecord();
            record.setScene(SCENE);
            record.setBusinessKey(UUID.randomUUID().toString().replace("-", ""));
            record.setCreatedBy(userId);
        }
        record.setSceneVar(String.valueOf(request.getGroupId()));
        record.setContent(content.toJSONString());
        record.setUpdatedBy(userId);

        record = universalRecordRepository.save(record);
        return CommonResponse.success(toDTO(record));
    }

    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        Optional<UniversalRecord> existing = universalRecordRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getCreatedBy())) {
            CommonException.NOT_ALLOWED.throwsError("邀请函不存在或无权限");
        }
        universalRecordRepository.deleteById(id);
        return CommonResponse.success();
    }

    private InvitationDTO toDTO(UniversalRecord record) {
        InvitationDTO dto = new InvitationDTO();
        dto.setId(record.getId());
        dto.setCreatedBy(record.getCreatedBy());
        if (record.getSceneVar() != null) {
            try {
                dto.setGroupId(Long.valueOf(record.getSceneVar()));
            } catch (NumberFormatException ignore) {
            }
        }
        JSONObject content = JSON.parseObject(record.getContent());
        if (content != null) {
            dto.setTitle(content.getString("title"));
            dto.setEventTime(content.getString("eventTime"));
            dto.setAddress(content.getString("address"));
            dto.setBody(content.getString("body"));
            dto.setCardStyle(content.getString("cardStyle"));
        }
        return dto;
    }

    public static class InvitationDTO {
        private Long id;
        private Long groupId;
        private Long createdBy;
        private String title;
        private String eventTime;
        private String address;
        private String body;
        private String cardStyle;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getEventTime() { return eventTime; }
        public void setEventTime(String eventTime) { this.eventTime = eventTime; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getCardStyle() { return cardStyle; }
        public void setCardStyle(String cardStyle) { this.cardStyle = cardStyle; }
    }

    public static class SaveInvitationRequest {
        private Long id;
        private Long groupId;
        private String title;
        private String eventTime;
        private String address;
        private String body;
        private String cardStyle;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getEventTime() { return eventTime; }
        public void setEventTime(String eventTime) { this.eventTime = eventTime; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getCardStyle() { return cardStyle; }
        public void setCardStyle(String cardStyle) { this.cardStyle = cardStyle; }
    }
}
