package com.funfun.schedule.controller.ai.dto;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 「微信 AI 打卡 SKILL」/active-list 请求：查询某成员某天的待打卡 / 已打卡列表。
 *
 * targetUserId 缺省时按当前登录用户处理；date 缺省今天。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AiCheckinActiveRequest extends BaseGroupUserRequest {
    private LocalDate date;
}
