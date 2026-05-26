package com.funfun.schedule.scheduleitem.handler;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.scheduleitem.ScheduleItemDateRules;
import com.funfun.schedule.scheduleitem.ScheduleItemTypeHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * 任务（task）：按重复区间判定；额外给前端补充 dueDate（到期日）。
 */
@Component
public class TaskTypeHandler implements ScheduleItemTypeHandler {

    @Override
    public Set<ScheduleItemType> supportedTypes() {
        return EnumSet.of(ScheduleItemType.task);
    }

    @Override
    public boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date) {
        return ScheduleItemDateRules.withinRepeatRange(item.getRepeatStartDay(), item.getRepeatEndDay(), date);
    }

    @Override
    public void decorate(ScheduleItemDTO item, LocalDate date, JSONObject showExtra) {
        showExtra.put("dueDate", ScheduleItemDateRules.dueDate(item, date).toString());
    }
}
