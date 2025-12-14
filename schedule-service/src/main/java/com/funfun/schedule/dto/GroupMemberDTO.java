package com.funfun.schedule.dto;

import lombok.Data;

@Data
public class GroupMemberDTO{
    private Long groupId; // 群组ID
    private Long userId; // 用户ID
    private String role; // 角色
    private UserInfoDTO userInfo;
    private String bindType;
}
