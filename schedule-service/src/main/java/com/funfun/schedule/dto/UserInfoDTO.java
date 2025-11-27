package com.funfun.schedule.dto;

import lombok.Data;

import java.util.Date;

/**
 * 用户信息响应 DTO（前端展示用）
 */
@Data
public class UserInfoDTO {
    private Long id; // 本地用户ID（透传给下游的ID）
    private String nickname; // 微信昵称
    private String avatarUrl; // 微信头像URL
    private Date createTime; // 账号创建时间
}