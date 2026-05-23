package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import com.funfun.schedule.enums.CloseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 「停止关注 / 恢复关注」请求：closeStatus = CLOSE 停止关注，OPEN 恢复关注。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CloseScheduleItemRequest extends BaseGroupUserRequest {
    private String id;
    private CloseStatus closeStatus;
}
