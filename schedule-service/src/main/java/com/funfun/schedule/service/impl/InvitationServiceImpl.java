package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.InvitationDTO;
import com.funfun.schedule.dto.ScheduleItemChange;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.scheduleitem.ScheduleItemSupport;
import com.funfun.schedule.service.InvitationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 邀请函业务实现：邀请函以 schedule_item 存储。
 * 发出的邀请 item_type=invSent；收到的邀请 item_type=invRecv，parentId 指向对应 invSent 记录。
 */
@Slf4j
@Service
public class InvitationServiceImpl implements InvitationService {

    private static final String TYPE_SENT = ScheduleItemType.invSent.name();
    private static final String TYPE_RECV = ScheduleItemType.invRecv.name();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ScheduleItemRepository scheduleItemRepository;

    @Autowired
    public InvitationServiceImpl(ScheduleItemRepository scheduleItemRepository) {
        this.scheduleItemRepository = scheduleItemRepository;
    }

    @Override
    public List<InvitationDTO> listForHome(Long userId, Long groupId) {
        List<InvitationDTO> result = new ArrayList<>();
        if (groupId != null) {
            List<ScheduleItem> sent = scheduleItemRepository
                    .findByGroupIdAndCreateByAndItemTypeOrderByCreateTimeDesc(groupId, userId, TYPE_SENT);
            for (ScheduleItem item : sent) {
                result.add(toDTO(item, "sent"));
            }
        }
        // 「我收到的」不限群组，符合「看到自己接受的邀请函」语义
        List<ScheduleItem> received = scheduleItemRepository
                .findByUserIdAndItemTypeOrderByCreateTimeDesc(userId, TYPE_RECV);
        for (ScheduleItem item : received) {
            result.add(toDTO(item, "received"));
        }
        return result;
    }

    @Override
    public InvitationDTO getById(Long id) {
        Optional<ScheduleItem> item = scheduleItemRepository.findById(id);
        if (item.isEmpty()) {
            return null;
        }
        ScheduleItem entity = item.get();
        String direction = TYPE_RECV.equals(entity.getItemType()) ? "received" : "sent";
        return toDTO(entity, direction);
    }

    @Override
    @Transactional
    public InvitationDTO saveSent(Long userId, InvitationDTO dto) {
        if (dto.getGroupId() == null) {
            CommonException.PARAM_INVALID.throwsError("groupId 不能为空");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            CommonException.PARAM_INVALID.throwsError("活动标题不能为空");
        }
        LocalDateTime start = parseTime(dto.getStartTime());
        LocalDateTime end = parseTime(dto.getEndTime());
        if (start == null || end == null) {
            CommonException.PARAM_INVALID.throwsError("活动开始时间和结束时间不能为空");
        }
        if (end.isBefore(start)) {
            CommonException.PARAM_INVALID.throwsError("结束时间不能早于开始时间");
        }

        ScheduleItem entity;
        // 更新前的旧值，用于级联判断
        LocalDateTime oldStart = null;
        LocalDateTime oldEnd = null;
        String oldLocation = null;
        boolean isUpdate = false;

        if (dto.getId() != null) {
            Optional<ScheduleItem> existing = scheduleItemRepository.findById(dto.getId());
            if (existing.isEmpty() || !TYPE_SENT.equals(existing.get().getItemType())) {
                CommonException.NOT_FOUND.throwsError("邀请函不存在");
            }
            entity = existing.get();
            if (!userId.equals(entity.getCreateBy())) {
                CommonException.NOT_ALLOWED.throwsError("无权限修改该邀请函");
            }
            oldStart = entity.getStartTime();
            oldEnd = entity.getEndTime();
            oldLocation = entity.getLocation();
            isUpdate = true;
        } else {
            entity = new ScheduleItem();
            entity.setItemType(TYPE_SENT);
            entity.setUserId(userId);
            entity.setGroupId(dto.getGroupId());
            entity.setRepeatType("none");
            // parentId=0 / closeStatus=OPEN 由实体字段默认值提供
        }

        entity.setItemTitle(dto.getTitle().trim());
        entity.setLocation(trimToNull(dto.getAddress()));
        entity.setStartTime(start);
        entity.setEndTime(end);
        // 重复字段对邀请函无意义，用活动起止日期兜底 NOT NULL 约束 / 兼容通用查询
        entity.setRepeatStartDay(start.toLocalDate());
        entity.setRepeatEndDay(end.toLocalDate());
        entity.setExtra(buildExtra(dto.getBody(), dto.getCardStyle(), dto.getSignature(), null));
        // 审计字段：新建盖创建+更新人/时间，更新只盖更新人/时间
        if (isUpdate) {
            ScheduleItemSupport.stampUpdate(entity, userId);
        } else {
            ScheduleItemSupport.stampCreate(entity, userId);
        }

        entity = scheduleItemRepository.save(entity);

        // 级联：时间或地点变更时，同步所有「收到的邀请」子记录
        if (isUpdate) {
            boolean timeChanged = !Objects.equals(oldStart, start) || !Objects.equals(oldEnd, end);
            boolean locationChanged = !Objects.equals(oldLocation, entity.getLocation());
            if (timeChanged || locationChanged) {
                cascadeToChildren(entity, timeChanged, locationChanged);
            }
        }

        return toDTO(entity, "sent");
    }

    @Override
    @Transactional
    public void deleteSent(Long userId, Long id) {
        Optional<ScheduleItem> existing = scheduleItemRepository.findById(id);
        if (existing.isEmpty() || !TYPE_SENT.equals(existing.get().getItemType())) {
            CommonException.NOT_FOUND.throwsError("邀请函不存在");
            return;
        }
        if (!userId.equals(existing.get().getCreateBy())) {
            CommonException.NOT_ALLOWED.throwsError("无权限删除该邀请函");
        }
        // 只删发出记录，受邀人各自的收到记录保留
        scheduleItemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public InvitationDTO accept(Long userId, Long invitationId, String recipientName) {
        if (invitationId == null) {
            CommonException.PARAM_INVALID.throwsError("invitationId 不能为空");
        }
        Optional<ScheduleItem> originOpt = scheduleItemRepository.findById(invitationId);
        if (originOpt.isEmpty() || !TYPE_SENT.equals(originOpt.get().getItemType())) {
            CommonException.NOT_FOUND.throwsError("邀请函不存在或已删除");
            return null;
        }
        ScheduleItem origin = originOpt.get();

        // 幂等：已收下过则直接返回已有记录
        List<ScheduleItem> existed = scheduleItemRepository
                .findByParentIdAndUserIdAndItemType(invitationId, userId, TYPE_RECV);
        if (!existed.isEmpty()) {
            return toDTO(existed.get(0), "received");
        }

        // 以服务端原邀请的当前数据拷贝，保证收下的是最新内容
        ScheduleItem recv = new ScheduleItem();
        recv.setItemType(TYPE_RECV);
        recv.setItemTitle(origin.getItemTitle());
        recv.setLocation(origin.getLocation());
        recv.setStartTime(origin.getStartTime());
        recv.setEndTime(origin.getEndTime());
        recv.setRepeatType("none");
        recv.setRepeatStartDay(origin.getRepeatStartDay());
        recv.setRepeatEndDay(origin.getRepeatEndDay());
        recv.setExtra(copyExtraWithRecipient(origin.getExtra(), recipientName));
        recv.setParentId(invitationId);
        recv.setUserId(userId);
        // 收到记录沿用原邀请的群组（受邀人可能不属于该群，仅作归属标记）
        recv.setGroupId(origin.getGroupId());
        ScheduleItemSupport.stampCreate(recv, userId);

        recv = scheduleItemRepository.save(recv);
        return toDTO(recv, "received");
    }

    /** 把原邀请的时间/地点变更同步到所有收到记录，并追加一条变更记录。 */
    private void cascadeToChildren(ScheduleItem origin, boolean timeChanged, boolean locationChanged) {
        List<ScheduleItem> children = scheduleItemRepository
                .findByParentIdAndItemType(origin.getId(), TYPE_RECV);
        if (children.isEmpty()) {
            return;
        }
        String summary = buildChangeSummary(origin, timeChanged, locationChanged);
        for (ScheduleItem child : children) {
            child.setStartTime(origin.getStartTime());
            child.setEndTime(origin.getEndTime());
            child.setLocation(origin.getLocation());
            child.setRepeatStartDay(origin.getRepeatStartDay());
            child.setRepeatEndDay(origin.getRepeatEndDay());
            child.setUpdateTime(new Date());

            ScheduleItemUpdateScope scope = ScheduleItemSupport.parseScope(child.getUpdateScope());
            if (scope.getChanges() == null) {
                scope.setChanges(new ArrayList<>());
            }
            ScheduleItemChange change = new ScheduleItemChange();
            change.setChangeTime(LocalDateTime.now());
            change.setSummary(summary);
            change.setStartTime(origin.getStartTime());
            change.setEndTime(origin.getEndTime());
            change.setLocation(origin.getLocation());
            scope.getChanges().add(change);
            child.setUpdateScope(ScheduleItemSupport.writeScope(scope));

            scheduleItemRepository.save(child);
        }
        log.info("invitation cascade: origin={} updated {} child(ren), summary={}",
                origin.getId(), children.size(), summary);
    }

    private String buildChangeSummary(ScheduleItem origin, boolean timeChanged, boolean locationChanged) {
        StringBuilder sb = new StringBuilder();
        if (timeChanged) {
            sb.append("时间调整为 ")
                    .append(formatTime(origin.getStartTime()))
                    .append(" ~ ")
                    .append(formatTime(origin.getEndTime()));
        }
        if (locationChanged) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append("地点调整为 ")
                    .append(origin.getLocation() == null ? "（待定）" : origin.getLocation());
        }
        return sb.toString();
    }

    // ---------- 转换 / 工具 ----------

    private InvitationDTO toDTO(ScheduleItem entity, String direction) {
        InvitationDTO dto = new InvitationDTO();
        dto.setId(entity.getId());
        dto.setGroupId(entity.getGroupId());
        dto.setCreatedBy(entity.getCreateBy());
        dto.setUserId(entity.getUserId());
        dto.setDirection(direction);
        dto.setParentId(entity.getParentId());
        dto.setTitle(entity.getItemTitle());
        dto.setStartTime(formatTime(entity.getStartTime()));
        dto.setEndTime(formatTime(entity.getEndTime()));
        dto.setAddress(entity.getLocation());

        JSONObject extra = ScheduleItemSupport.parseExtra(entity.getExtra());
        if (extra != null) {
            dto.setBody(extra.getString("body"));
            dto.setCardStyle(extra.getString("cardStyle"));
            dto.setSignature(extra.getString("signature"));
            dto.setRecipientName(extra.getString("recipientName"));
        }

        ScheduleItemUpdateScope scope = ScheduleItemSupport.parseScope(entity.getUpdateScope());
        if (scope.getChanges() != null && !scope.getChanges().isEmpty()) {
            dto.setChanges(scope.getChanges());
        }
        return dto;
    }

    /** 邀请函展示字段塞进 extra（body/cardStyle/signature/recipientName）。 */
    private String buildExtra(String body, String cardStyle, String signature, String recipientName) {
        JSONObject extra = new JSONObject();
        extra.put("body", body);
        extra.put("cardStyle", cardStyle);
        extra.put("signature", signature);
        if (recipientName != null) {
            extra.put("recipientName", recipientName);
        }
        return ScheduleItemSupport.writeExtra(extra);
    }

    /** 拷贝原邀请 extra 并写入收下人的称呼。 */
    private String copyExtraWithRecipient(String originExtra, String recipientName) {
        JSONObject extra = ScheduleItemSupport.parseExtra(originExtra);
        if (extra == null) {
            extra = new JSONObject();
        }
        if (recipientName != null && !recipientName.trim().isEmpty()) {
            extra.put("recipientName", recipientName.trim());
        }
        return ScheduleItemSupport.writeExtra(extra);
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        try {
            // "yyyy-MM-dd HH:mm" 或 "yyyy-MM-dd HH:mm:ss"
            if (v.length() == 16) {
                return LocalDateTime.parse(v, FMT);
            }
            if (v.length() >= 19) {
                return LocalDateTime.parse(v.substring(0, 16), FMT);
            }
            // 仅日期 "yyyy-MM-dd"
            if (v.length() == 10) {
                return LocalDate.parse(v).atStartOfDay();
            }
            // 兜底尝试 ISO
            return LocalDateTime.parse(v.replace(" ", "T"));
        } catch (Exception e) {
            CommonException.PARAM_INVALID.throwsError("时间格式有误: " + value);
            return null;
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(FMT);
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
