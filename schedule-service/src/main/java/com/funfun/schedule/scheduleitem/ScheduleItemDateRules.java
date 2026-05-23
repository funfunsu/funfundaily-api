package com.funfun.schedule.scheduleitem;

import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.util.DateUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 日程项的日期判定规则（纯函数，无状态）。
 *
 * <p>把原先散落在 ScheduleItemServiceImpl 里的几个类型相关日期判定集中到这里，
 * 供各 {@link ScheduleItemTypeHandler} 与查询引擎共用，避免重复实现。
 */
public final class ScheduleItemDateRules {

    private ScheduleItemDateRules() {
    }

    /**
     * 时间窗判定：日程/邀请这类「定点事件」用实际起止时间是否落在某天内来判断。
     */
    public static boolean withinTimeWindow(LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        if (startTime == null || endTime == null || date == null) {
            return false;
        }
        LocalDateTime startOfDay = DateUtil.getStartOfDay(date.atStartOfDay());
        LocalDateTime endOfDay = DateUtil.getEndOfDay(date.atStartOfDay());
        return (startTime.isAfter(startOfDay) && startTime.isBefore(endOfDay))
                || (endTime.isAfter(startOfDay) && endTime.isBefore(endOfDay));
    }

    /**
     * 重复区间判定：任务/目标/事件这类「按周期重复」的项用 repeatStartDay~repeatEndDay 区间判断。
     */
    public static boolean withinRepeatRange(LocalDate repeatStartDay, LocalDate repeatEndDay, LocalDate date) {
        if (repeatStartDay == null) {
            return true;
        }
        if (repeatEndDay == null) {
            return !date.isBefore(repeatStartDay);
        }
        return !date.isBefore(repeatStartDay) && (date.isBefore(repeatEndDay) || date.equals(repeatEndDay));
    }

    /**
     * 任务到期日：按重复类型推算 taskTime 所属周期的最后一天，并以 repeatEndDay 封顶。
     */
    public static LocalDate dueDate(ScheduleItemDTO item, LocalDate taskTime) {
        LocalDate dueDate;
        switch (item.getRepeatType()) {
            case daily:
                dueDate = taskTime;
                break;
            case weekly:
                dueDate = taskTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                break;
            case yearly:
                dueDate = taskTime.with(TemporalAdjusters.lastDayOfYear());
                break;
            case monthly:
                dueDate = taskTime.with(TemporalAdjusters.lastDayOfMonth());
                break;
            default:
                dueDate = item.getRepeatEndDay();
        }
        if (item.getRepeatEndDay().isBefore(dueDate)) {
            return item.getRepeatEndDay();
        }
        return dueDate;
    }
}
