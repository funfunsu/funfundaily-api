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
 * 邀请（invSent/invRecv）：邀请是定点事件，按实际起止时间窗判定（与日程一致）。
 * 用于直接按邀请类型查询的场景；作为日程表关联项时由日程 Handler 统一按时间窗判定。
 */
@Component
public class InvitationTypeHandler implements ScheduleItemTypeHandler {

    @Override
    public Set<ScheduleItemType> supportedTypes() {
        return EnumSet.of(ScheduleItemType.invSent, ScheduleItemType.invRecv);
    }

    @Override
    public boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date) {
        return ScheduleItemDateRules.withinTimeWindow(item.getStartTime(), item.getEndTime(), date);
    }
}
