package com.funfun.schedule.controller.ai.dto;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 「微信 AI 打卡 SKILL」/streak 请求：查询某任务的连续打卡天数与本月统计。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AiCheckinStreakRequest extends BaseGroupUserRequest {
    private String taskId;
}
