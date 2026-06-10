package com.funfun.schedule.constants;

public class TaskConstants {


    public static final String scoreKey = "score";
    public static final String taskTypeKey = "taskType";
    public static final String totalCntKey = "totalCount";
    // 积分模式：each=单次打卡即得分；full（默认/缺省）=周期全部完成才得分
    public static final String scoreModeKey = "scoreMode";
    public static final String SCORE_MODE_EACH = "each";
    public static final String SCORE_MODE_FULL = "full";
    // 每日打卡上限：>=1 时限制同一自然日打卡次数；缺省/<=0 表示不限制
    public static final String dailyLimitKey = "dailyLimit";
    // 周期全部完成额外奖励积分：>0 时在周期达成那一次额外发放
    public static final String bonusScoreKey = "bonusScore";
}
