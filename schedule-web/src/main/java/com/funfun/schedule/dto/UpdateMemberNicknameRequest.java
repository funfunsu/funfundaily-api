package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员/群主修改群内未绑定微信成员的昵称。
 * groupId 用于 @RequiredDataPermission 校验调用方在该群的角色；
 * targetUserId 为被修改的成员 userId（必须 openid 为空即 bindType=None）。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateMemberNicknameRequest extends BaseGroupRequest {
    private String targetUserId;
    private String nickname;
}
