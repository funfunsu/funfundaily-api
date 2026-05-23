package com.funfun.schedule.scheduleitem;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.enums.ScheduleItemType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 日程项「类型策略」。每个场景（schedule / task / 邀请 等）实现一个 Handler，
 * 把该类型特有的判定/装饰/关联规则集中在一处，替代原先散落在 service 里的 if/switch。
 *
 * <p>新增一种场景时只需新增一个 Handler 实现并声明 {@link #supportedTypes()}，
 * 由 {@link ScheduleItemTypeHandlerRegistry} 自动按类型分发，无需改动查询引擎。
 */
public interface ScheduleItemTypeHandler {

    /** 该 Handler 负责的类型集合。 */
    Set<ScheduleItemType> supportedTypes();

    /**
     * 非重复（repeatType=none）的项是否应在指定日期出现。
     * 日程/邀请用「时间窗」，任务/目标/事件用「重复区间」。
     */
    boolean occursOnNonRepeating(ScheduleItemDTO item, LocalDate date);

    /**
     * 给前端展示用的 showExtra 补充类型特有字段（如任务的 dueDate）。默认不补充。
     */
    default void decorate(ScheduleItemDTO item, LocalDate date, JSONObject showExtra) {
    }

    /**
     * 查询该类型时需顺带纳入的关联类型（如「日程表」顺带展示「收到的邀请」invRecv）。默认无。
     */
    default List<ScheduleItemType> companionTypes() {
        return Collections.emptyList();
    }
}
