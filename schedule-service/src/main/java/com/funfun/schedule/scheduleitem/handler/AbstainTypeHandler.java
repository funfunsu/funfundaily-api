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
 * 戒断事件（abstain）：戒断日记的一种长期持续事件，start_time=创建时间、end_time=戒断目标结束时间，
 * 按实际起止时间窗判定（与日程/邀请一致）。
 *
 * <p>注意：戒断事件列表走专用扁平 active 列表（{@code /api/schedule/active/list}，
 * 不按天展开），本 Handler 仅在 abstain 偶尔进入按天展开查询时提供时间窗判定，保持类型策略一致。
 */
@Component
public class AbstainTypeHandler implements ScheduleItemTypeHandler {

    @Override
    public Set<ScheduleItemType> supportedTypes() {
        return EnumSet.of(ScheduleItemType.abstain);
    }

    @Override
    public boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date) {
        return ScheduleItemDateRules.withinTimeWindow(item.getStartTime(), item.getEndTime(), date);
    }
}
