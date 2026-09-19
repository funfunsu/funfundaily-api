package com.funfun.schedule.controller.ai.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 「微信 AI 打卡 SKILL」原子组件渲染所需的结构化数据。
 * 每个 card 都带 type 字段，前端原子组件按 type 分发渲染。
 *
 * 注意：传递给 AI 的"事实/动作"放在 AiResponseEnvelope.fact/action，
 * 这里只放给前端组件渲染用的结构化数据。
 */
public final class AiCheckinCards {

    private AiCheckinCards() {}

    /** 待打卡/已打卡列表（某成员某天）。 */
    @Data
    public static class ActiveList {
        private final String type = "checkin_active_list";
        private LocalDate date;
        private int pendingCount;
        private int completedCount;
        private List<TaskItem> pending;
        private List<TaskItem> completed;
    }

    /** 单次打卡结果。 */
    @Data
    public static class CompleteResult {
        private final String type = "checkin_complete_result";
        private Long recordId;
        private Long taskId;
        private String title;
        private LocalDate date;
        /** 含本次的连续天数。 */
        private int currentStreak;
        /** 本月已打卡天数。 */
        private int monthCheckinDays;
        /** 还有哪些今日待打卡（供 AI 顺势引导继续）。 */
        private List<TaskItem> remainingPending;
    }

    /** 某任务的连续打卡统计。 */
    @Data
    public static class Streak {
        private final String type = "checkin_streak";
        private Long taskId;
        private String title;
        private int currentStreak;
        private int monthCheckinDays;
        /** 最近 30 天的打卡日期，前端可用于绘热力/日历图。 */
        private List<LocalDate> recentDates;
    }

    /** 极简任务项：仅暴露 AI/组件需要的最少信息。 */
    @Data
    public static class TaskItem {
        private Long taskId;
        private String title;
        /** 用于前端图标区分（task/goal 等）。 */
        private String itemType;

        public TaskItem() {}

        public TaskItem(Long taskId, String title, String itemType) {
            this.taskId = taskId;
            this.title = title;
            this.itemType = itemType;
        }
    }
}
