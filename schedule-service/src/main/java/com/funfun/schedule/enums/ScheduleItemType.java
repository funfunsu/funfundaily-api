package com.funfun.schedule.enums;

public enum ScheduleItemType {
    schedule,
    task,
    goal,
    event,
    /** 发出的邀请（邀请函创建者本人持有的记录） */
    invSent,
    /** 收到的邀请（受邀人「收下」后持有的记录，parentId 指向对应的 invSent 记录） */
    invRecv,
    /** 家庭月度计划事件（一次性如「2026年11月装修」/ 周期性如「每年5月家庭财务测算」） */
    monthlyPlan,
    /** 戒断事件（戒断日记：start_time=创建时间，end_time=戒断目标结束时间；按天反馈达成/破戒） */
    abstain
}
