package com.funfun.schedule.scheduleitem;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.CloseStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 日程项的共享辅助：extra / updateScope 的 JSON 编解码，以及实体审计字段/默认值的统一盖章。
 *
 * <p>把原先散落在 InvitationServiceImpl、ScheduleItemServiceImpl、ScheduleItemMapper 里
 * 各写一遍的 JSON 解析与字段初始化集中到这里，保证不同场景的行为一致。
 */
@Slf4j
public final class ScheduleItemSupport {

    private ScheduleItemSupport() {
    }

    // ---------- extra (JSON 自由扩展字段) ----------

    public static JSONObject parseExtra(String extra) {
        if (extra == null || extra.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(extra);
        } catch (Exception e) {
            log.warn("scheduleItem extra parse failed: {}", extra, e);
            return null;
        }
    }

    public static String writeExtra(JSONObject extra) {
        return extra == null ? null : extra.toJSONString();
    }

    // ---------- updateScope (任务完成快照 / 邀请变更记录) ----------

    /** 解析 updateScope；为空或解析失败时返回一个空对象，便于调用方直接 append。 */
    public static ScheduleItemUpdateScope parseScope(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return new ScheduleItemUpdateScope();
        }
        try {
            ScheduleItemUpdateScope parsed = JSON.parseObject(scope, ScheduleItemUpdateScope.class);
            return parsed == null ? new ScheduleItemUpdateScope() : parsed;
        } catch (Exception e) {
            log.warn("scheduleItem updateScope parse failed: {}", scope, e);
            return new ScheduleItemUpdateScope();
        }
    }

    public static String writeScope(ScheduleItemUpdateScope scope) {
        return scope == null ? null : JSON.toJSONString(scope);
    }

    // ---------- 审计字段 / 默认值 ----------

    /** 新建：盖创建/更新人与时间，并兜底必填默认值（关闭状态、parentId）。 */
    public static void stampCreate(ScheduleItem entity, Long userId) {
        Date now = new Date();
        entity.setCreateBy(userId);
        entity.setUpdateBy(userId);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        applyDefaults(entity);
    }

    /** 更新：盖更新人与时间。 */
    public static void stampUpdate(ScheduleItem entity, Long userId) {
        entity.setUpdateBy(userId);
        entity.setUpdateTime(new Date());
    }

    /** 兜底 NOT NULL 默认值（MapStruct 可能用 DTO 的 null 覆盖实体初始值）。 */
    public static void applyDefaults(ScheduleItem entity) {
        if (entity.getCloseStatus() == null) {
            entity.setCloseStatus(CloseStatus.OPEN);
        }
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }
    }
}
