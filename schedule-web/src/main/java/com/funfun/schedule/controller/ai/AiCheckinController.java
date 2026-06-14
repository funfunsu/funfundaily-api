package com.funfun.schedule.controller.ai;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.anno.RequiredDataPermission;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.controller.ai.dto.AiCheckinActiveRequest;
import com.funfun.schedule.controller.ai.dto.AiCheckinCards;
import com.funfun.schedule.controller.ai.dto.AiCheckinCompleteRequest;
import com.funfun.schedule.controller.ai.dto.AiCheckinStreakRequest;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.QueryScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.AiResponseEnvelope;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.CheckinService;
import com.funfun.schedule.service.ScheduleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 「微信小程序 AI 开发模式 - 打卡 SKILL」服务端原子接口。
 *
 * 命名空间 /api/ai/* 与常规 /api/* 分离：
 *   - 返回值统一封装为 AiResponseEnvelope（fact / action / card），不污染常规接口
 *   - 复用现有 service + @RequiredDataPermission，权限/数据隔离一致
 *   - 文案集中在 AiCheckinNarrator
 *
 * 端点：
 *   POST /api/ai/checkin/active-list  某成员某天的打卡列表（待打卡 + 已打卡）
 *   POST /api/ai/checkin/complete     为某任务执行打卡
 *   POST /api/ai/checkin/streak       某任务的连续打卡天数与本月统计
 */
@RestController
@RequestMapping("/api/ai/checkin")
public class AiCheckinController {

    /** 连续天数回溯窗口（足够覆盖绝大多数连续打卡场景）。 */
    private static final int STREAK_LOOKBACK_DAYS = 90;

    @Autowired
    private ScheduleItemService scheduleItemService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private AiCheckinNarrator narrator;

    @PostMapping("/active-list")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<AiResponseEnvelope<AiCheckinCards.ActiveList>> activeList(
            @RequestBody AiCheckinActiveRequest request) throws ParseException {
        Long groupId = requireGroupId(request.getGroupId());
        Long userId = resolveTargetUserId(request.getTargetUserId());
        LocalDate date = request.getDate() == null ? LocalDate.now() : request.getDate();

        AiCheckinCards.ActiveList card = buildActiveList(groupId, userId, date);
        String fact = narrator.narrateActiveListFact(date, card);
        String action = narrator.narrateActiveListAction(card);
        return CommonResponse.success(AiResponseEnvelope.of(fact, action, card));
    }

    @PostMapping("/complete")
    @RequiredDataPermission
    public CommonResponse<AiResponseEnvelope<AiCheckinCards.CompleteResult>> complete(
            @RequestBody AiCheckinCompleteRequest request) throws ParseException {
        Long groupId = requireGroupId(request.getGroupId());
        Long userId = resolveTargetUserId(request.getTargetUserId());
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            CommonException.DATA_INVALID.throwsError("taskId is required");
        }
        Long taskId = Long.valueOf(request.getTaskId());
        LocalDate date = request.getDate() == null ? LocalDate.now() : request.getDate();

        // 拉一次任务定义用于标题展示
        ScheduleItemDTO taskItem = scheduleItemService.getScheduleItemById(taskId);
        if (taskItem == null) {
            CommonException.DATA_INVALID.throwsError("task not found");
        }

        // 执行打卡（复用现有 service，保持积分/校验/事务一致）
        CheckinRecordDTO dto = new CheckinRecordDTO();
        dto.setGroupId(groupId);
        dto.setUserId(userId);
        dto.setOperatorId(UserContext.getUserId());
        dto.setTaskId(taskId);
        dto.setTaskTime(date.atStartOfDay());
        dto.setExtra(new JSONObject());
        Long recordId = checkinService.performCheckin(dto);

        // 统计：连续天数 + 本月已打卡 + 剩余 pending
        StreakStats stats = computeStreak(groupId, userId, taskId, date);
        AiCheckinCards.ActiveList today = buildActiveList(groupId, userId, date);

        AiCheckinCards.CompleteResult card = new AiCheckinCards.CompleteResult();
        card.setRecordId(recordId);
        card.setTaskId(taskId);
        card.setTitle(taskItem.getItemTitle());
        card.setDate(date);
        card.setCurrentStreak(stats.streak);
        card.setMonthCheckinDays(stats.monthDays);
        card.setRemainingPending(today.getPending());

        String fact = narrator.narrateCompleteFact(card);
        String action = narrator.narrateCompleteAction(card);
        return CommonResponse.success(AiResponseEnvelope.of(fact, action, card));
    }

    @PostMapping("/streak")
    @RequiredDataPermission(allowRole = {GroupRole.Admin, GroupRole.Member})
    public CommonResponse<AiResponseEnvelope<AiCheckinCards.Streak>> streak(
            @RequestBody AiCheckinStreakRequest request) {
        Long groupId = requireGroupId(request.getGroupId());
        Long userId = resolveTargetUserId(request.getTargetUserId());
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            CommonException.DATA_INVALID.throwsError("taskId is required");
        }
        Long taskId = Long.valueOf(request.getTaskId());

        ScheduleItemDTO taskItem = scheduleItemService.getScheduleItemById(taskId);
        if (taskItem == null) {
            CommonException.DATA_INVALID.throwsError("task not found");
        }
        LocalDate today = LocalDate.now();
        StreakStats stats = computeStreak(groupId, userId, taskId, today);

        AiCheckinCards.Streak card = new AiCheckinCards.Streak();
        card.setTaskId(taskId);
        card.setTitle(taskItem.getItemTitle());
        card.setCurrentStreak(stats.streak);
        card.setMonthCheckinDays(stats.monthDays);
        card.setRecentDates(stats.recentDates);

        String fact = narrator.narrateStreakFact(card);
        String action = narrator.narrateStreakAction(card);
        return CommonResponse.success(AiResponseEnvelope.of(fact, action, card));
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private AiCheckinCards.ActiveList buildActiveList(Long groupId, Long userId, LocalDate date) throws ParseException {
        QueryScheduleItemDTO query = new QueryScheduleItemDTO();
        query.setFromDate(date);
        query.setToDate(date);
        query.setScheduleItemType(ScheduleItemType.task);
        List<ScheduleListItemDTO> grouped =
                scheduleItemService.getScheduleItemsByDateRange(groupId, userId, query);

        // 当天的所有打卡任务（按 taskId 去重，避免按天分组重复）
        List<ScheduleItemDTO> tasks = grouped == null ? Collections.emptyList()
                : grouped.stream()
                .flatMap(g -> g.getSchedules() == null ? java.util.stream.Stream.empty() : g.getSchedules().stream())
                .collect(Collectors.toMap(ScheduleItemDTO::getId, t -> t, (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());

        // 当天已打卡的 taskId 集合
        Set<Long> completedIds = collectCompletedTaskIds(groupId, userId, date, date);

        List<AiCheckinCards.TaskItem> pending = new ArrayList<>();
        List<AiCheckinCards.TaskItem> completed = new ArrayList<>();
        for (ScheduleItemDTO t : tasks) {
            AiCheckinCards.TaskItem item = new AiCheckinCards.TaskItem(t.getId(), t.getItemTitle(), t.getItemType());
            if (completedIds.contains(t.getId())) {
                completed.add(item);
            } else {
                pending.add(item);
            }
        }

        AiCheckinCards.ActiveList card = new AiCheckinCards.ActiveList();
        card.setDate(date);
        card.setPending(pending);
        card.setCompleted(completed);
        card.setPendingCount(pending.size());
        card.setCompletedCount(completed.size());
        return card;
    }

    private Set<Long> collectCompletedTaskIds(Long groupId, Long userId, LocalDate from, LocalDate to) {
        List<CheckinRecordDTO> records = checkinService.getRecordList(groupId, userId, null, from, to);
        if (records == null || records.isEmpty()) {
            return Collections.emptySet();
        }
        return records.stream().map(CheckinRecordDTO::getTaskId).collect(Collectors.toSet());
    }

    private StreakStats computeStreak(Long groupId, Long userId, Long taskId, LocalDate anchor) {
        LocalDate from = anchor.minusDays(STREAK_LOOKBACK_DAYS);
        List<CheckinRecordDTO> records =
                checkinService.getRecordList(groupId, userId, taskId, from, anchor);

        Set<LocalDate> doneDates = records == null ? Collections.emptySet()
                : records.stream()
                .map(r -> r.getTaskTime() == null ? null : r.getTaskTime().toLocalDate())
                .filter(d -> d != null)
                .collect(Collectors.toSet());

        // 连续天数：从 anchor 往前数；若 anchor 当天未打卡，则尝试从 anchor-1 起算（容错"昨天结束"场景）
        int streak = 0;
        LocalDate cursor = anchor;
        if (!doneDates.contains(cursor)) {
            cursor = anchor.minusDays(1);
        }
        while (doneDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        // 本月已打卡天数（包含 anchor 当天）
        LocalDate monthStart = anchor.withDayOfMonth(1);
        long monthDays = doneDates.stream()
                .filter(d -> !d.isBefore(monthStart) && !d.isAfter(anchor))
                .count();

        // 最近 30 天的打卡日期（倒序）
        LocalDate recentStart = anchor.minusDays(29);
        List<LocalDate> recent = doneDates.stream()
                .filter(d -> !d.isBefore(recentStart) && !d.isAfter(anchor))
                .sorted(java.util.Comparator.reverseOrder())
                .collect(Collectors.toList());

        StreakStats s = new StreakStats();
        s.streak = streak;
        s.monthDays = (int) monthDays;
        s.recentDates = recent;
        return s;
    }

    private static Long requireGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            CommonException.DATA_INVALID.throwsError("groupId is required");
        }
        return Long.valueOf(groupId);
    }

    private static Long resolveTargetUserId(String targetUserId) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            return UserContext.getUserId();
        }
        return Long.valueOf(targetUserId);
    }

    private static final class StreakStats {
        int streak;
        int monthDays;
        List<LocalDate> recentDates;
    }
}
