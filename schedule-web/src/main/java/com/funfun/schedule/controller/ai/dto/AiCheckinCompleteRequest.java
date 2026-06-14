package com.funfun.schedule.controller.ai.dto;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 「微信 AI 打卡 SKILL」/complete 请求：为某个任务执行打卡。
 *
 * taskId 必填（即 scheduleItem.id）；date 缺省今天；targetUserId 缺省当前登录用户。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AiCheckinCompleteRequest extends BaseGroupUserRequest {
    private String taskId;
    private LocalDate date;
}
