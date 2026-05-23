package com.funfun.schedule.scheduleitem;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.enums.RepeatType;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.scheduleitem.handler.InvitationTypeHandler;
import com.funfun.schedule.scheduleitem.handler.RepeatRangeTypeHandler;
import com.funfun.schedule.scheduleitem.handler.ScheduleTypeHandler;
import com.funfun.schedule.scheduleitem.handler.TaskTypeHandler;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日程项类型策略（Handler / Registry / DateRules）的单元测试。
 * 锁定从 ScheduleItemServiceImpl 抽取出来的类型分支行为，确保重构未改变语义。
 */
class ScheduleItemTypeHandlerTest {

    private final RepeatRangeTypeHandler repeatHandler = new RepeatRangeTypeHandler();
    private final ScheduleItemTypeHandlerRegistry registry = new ScheduleItemTypeHandlerRegistry(
            List.of(new ScheduleTypeHandler(), new TaskTypeHandler(), repeatHandler, new InvitationTypeHandler()),
            repeatHandler);

    private ScheduleItemDTO item(RepeatType repeatType, LocalDateTime start, LocalDateTime end,
                                 LocalDate repeatStart, LocalDate repeatEnd) {
        ScheduleItemDTO dto = new ScheduleItemDTO();
        dto.setId(1L);
        dto.setRepeatType(repeatType);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setRepeatStartDay(repeatStart);
        dto.setRepeatEndDay(repeatEnd);
        return dto;
    }

    @Test
    void registryDispatchesByType() {
        assertInstanceOf(ScheduleTypeHandler.class, registry.get(ScheduleItemType.schedule));
        assertInstanceOf(TaskTypeHandler.class, registry.get(ScheduleItemType.task));
        assertInstanceOf(RepeatRangeTypeHandler.class, registry.get(ScheduleItemType.goal));
        assertInstanceOf(RepeatRangeTypeHandler.class, registry.get(ScheduleItemType.event));
        assertInstanceOf(InvitationTypeHandler.class, registry.get(ScheduleItemType.invRecv));
    }

    @Test
    void scheduleUsesTimeWindow_andCompanionsIncludeInvRecv() {
        ScheduleItemTypeHandler handler = registry.get(ScheduleItemType.schedule);
        ScheduleItemDTO ev = item(RepeatType.none,
                LocalDateTime.of(2026, 6, 1, 18, 0), LocalDateTime.of(2026, 6, 1, 20, 0), null, null);

        assertTrue(handler.occursOnNonRepeating(ev, LocalDate.of(2026, 6, 1)));
        assertFalse(handler.occursOnNonRepeating(ev, LocalDate.of(2026, 6, 2)));
        assertTrue(handler.companionTypes().contains(ScheduleItemType.invRecv));
    }

    @Test
    void invitationUsesTimeWindow() {
        ScheduleItemTypeHandler handler = registry.get(ScheduleItemType.invRecv);
        ScheduleItemDTO ev = item(RepeatType.none,
                LocalDateTime.of(2026, 9, 15, 18, 0), LocalDateTime.of(2026, 9, 15, 20, 0), null, null);

        assertTrue(handler.occursOnNonRepeating(ev, LocalDate.of(2026, 9, 15)));
        assertFalse(handler.occursOnNonRepeating(ev, LocalDate.of(2026, 9, 16)));
        assertTrue(handler.companionTypes().isEmpty());
    }

    @Test
    void taskUsesRepeatRange_andDecoratesDueDate() {
        ScheduleItemTypeHandler handler = registry.get(ScheduleItemType.task);
        ScheduleItemDTO task = item(RepeatType.none, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        // 重复区间判定（非时间窗）
        assertTrue(handler.occursOnNonRepeating(task, LocalDate.of(2026, 6, 15)));
        assertFalse(handler.occursOnNonRepeating(task, LocalDate.of(2026, 7, 1)));

        // dueDate 装饰：weekly → 当周周日
        ScheduleItemDTO weekly = item(RepeatType.weekly, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        JSONObject showExtra = new JSONObject();
        handler.decorate(weekly, LocalDate.of(2026, 6, 3), showExtra); // 2026-06-03 周三 → 当周周日 06-07
        assertEquals("2026-06-07", showExtra.getString("dueDate"));
    }

    @Test
    void dateRulesDueDateRespectsRepeatEndCap() {
        ScheduleItemDTO monthly = item(RepeatType.monthly, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20));
        // monthly 本应到月底 06-30，但被 repeatEndDay(06-20) 封顶
        assertEquals(LocalDate.of(2026, 6, 20),
                ScheduleItemDateRules.dueDate(monthly, LocalDate.of(2026, 6, 10)));
    }

    @Test
    void unknownTypeFallsBackToRepeatRange() {
        // goal/event 等未单独定制装饰：兜底为重复区间，不抛异常、无 dueDate
        ScheduleItemTypeHandler handler = registry.get(ScheduleItemType.goal);
        ScheduleItemDTO goal = item(RepeatType.none, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        JSONObject showExtra = new JSONObject();
        handler.decorate(goal, LocalDate.of(2026, 6, 1), showExtra);
        assertFalse(showExtra.containsKey("dueDate"));
        assertTrue(handler.occursOnNonRepeating(goal, LocalDate.of(2026, 6, 1)));
    }
}
