package com.funfun.schedule.dto.openapi;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放接口打卡（checkInTask）请求体。
 *
 * <p>groupId 由 Bearer Token 决定；如此处显式传入则必须与令牌绑定的群组一致。
 */
@Data
public class OpenCheckinRequest {

    /** 任务 ID（必填）。 */
    private Long taskId;

    /** 打卡成员 ID（可选，默认任务归属成员）。 */
    private Long userId;

    /** 可选的群组 ID 校验值（须与令牌绑定群组一致）。 */
    private Long groupId;

    /** 打卡所属时间（可选，默认当天）。 */
    private LocalDateTime taskTime;
}
