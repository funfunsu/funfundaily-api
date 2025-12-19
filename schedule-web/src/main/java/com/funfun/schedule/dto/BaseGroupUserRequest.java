package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseGroupUserRequest extends BaseGroupRequest{
    private String targetUserId; // 通常由后端从安全上下文获取
}
