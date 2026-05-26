package com.funfun.schedule.scheduleitem.handler;

import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.scheduleitem.ScheduleItemDateRules;
import com.funfun.schedule.scheduleitem.ScheduleItemTypeHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 日程（schedule）：按实际起止时间窗判定；日程表同时展示「收到的邀请」(invRecv)。
 */
@Component
public class ScheduleTypeHandler implements ScheduleItemTypeHandler {

    @Override
    public Set<ScheduleItemType> supportedTypes() {
        return EnumSet.of(ScheduleItemType.schedule);
    }

    @Override
    public boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date) {
        return ScheduleItemDateRules.withinTimeWindow(item.getStartTime(), item.getEndTime(), date);
    }

    @Override
    public List<ScheduleItemType> companionTypes() {
        return List.of(ScheduleItemType.invRecv);
    }
}
