package com.funfun.schedule.scheduleitem.handler;

import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.scheduleitem.ScheduleItemDateRules;
import com.funfun.schedule.scheduleitem.ScheduleItemTypeHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * 目标（goal）/事件（event）：按重复区间判定，无额外装饰。
 * 同时作为 {@link com.funfun.schedule.scheduleitem.ScheduleItemTypeHandlerRegistry} 的兜底 Handler
 * （未显式登记的类型按「重复区间」处理，与原 else 分支行为一致）。
 */
@Component
public class RepeatRangeTypeHandler implements ScheduleItemTypeHandler {

    @Override
    public Set<ScheduleItemType> supportedTypes() {
        return EnumSet.of(ScheduleItemType.goal, ScheduleItemType.event);
    }

    @Override
    public boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date) {
        return ScheduleItemDateRules.withinRepeatRange(item.getRepeatStartDay(), item.getRepeatEndDay(), date);
    }
}
