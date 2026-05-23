package com.funfun.schedule.enums;

public enum ScheduleItemType {
    schedule,
    task,
    goal,
    event,
    /** 发出的邀请（邀请函创建者本人持有的记录） */
    invSent,
    /** 收到的邀请（受邀人「收下」后持有的记录，parentId 指向对应的 invSent 记录） */
    invRecv
}
