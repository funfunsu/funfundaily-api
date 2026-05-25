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
 * 月度计划（monthlyPlan）：家庭按月规划的一次性 / 周期性事件。
 *
 * <p>非重复项按重复区间判定（与 goal/event 一致）。月度计划页面主要走「原始列表」端点
 * （{@code /api/schedule/plan/list}）在前端按月归属，因此这里只作为标准按天展开路径的兜底，
 * 保证即便经普通 {@code /list} 查询也有明确、可预期的行为。
 */
@Component
public class MonthlyPlanTypeHandler implements ScheduleItemTypeHandler {

    @Override
    public Set<ScheduleItemType> supportedTypes() {
        return EnumSet.of(ScheduleItemType.monthlyPlan);
    }

    @Override
    public boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date) {
        return ScheduleItemDateRules.withinRepeatRange(item.getRepeatStartDay(), item.getRepeatEndDay(), date);
    }
}
