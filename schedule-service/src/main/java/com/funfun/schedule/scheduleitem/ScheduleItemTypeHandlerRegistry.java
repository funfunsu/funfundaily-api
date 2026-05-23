package com.funfun.schedule.scheduleitem;

import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.scheduleitem.handler.RepeatRangeTypeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按 {@link ScheduleItemType} 分发到对应的 {@link ScheduleItemTypeHandler}。
 * 启动时收集所有 Handler bean 并按其 supportedTypes 建表；未登记的类型回退到「重复区间」兜底 Handler。
 */
@Component
public class ScheduleItemTypeHandlerRegistry {

    private final Map<ScheduleItemType, ScheduleItemTypeHandler> handlers = new EnumMap<>(ScheduleItemType.class);
    private final ScheduleItemTypeHandler defaultHandler;

    @Autowired
    public ScheduleItemTypeHandlerRegistry(List<ScheduleItemTypeHandler> allHandlers,
                                           RepeatRangeTypeHandler defaultHandler) {
        for (ScheduleItemTypeHandler handler : allHandlers) {
            for (ScheduleItemType type : handler.supportedTypes()) {
                handlers.put(type, handler);
            }
        }
        this.defaultHandler = defaultHandler;
    }

    public ScheduleItemTypeHandler get(ScheduleItemType type) {
        ScheduleItemTypeHandler handler = handlers.get(type);
        return handler != null ? handler : defaultHandler;
    }
}
