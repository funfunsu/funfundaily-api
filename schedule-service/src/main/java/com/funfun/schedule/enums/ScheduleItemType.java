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
    monthlyPlan
}
