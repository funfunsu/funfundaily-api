package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邀请绑定分享内容：把当前微信用户绑定到一个未绑定 openid 的占位成员账号上。
 * targetUserId 即占位成员的 userId；recipient 接受分享后，他的 openid 会从其当前 wx 用户转移到该占位用户上。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BindMemberShareDTO extends BaseGroupRequest {
    private String groupName;
    private String targetUserId;
    private String targetNickname;
}
