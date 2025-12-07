package com.funfun.schedule.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求参数（小程序端传递code）
 */
@Data
public class LoginRequest {
    @NotBlank(message = "code不能为空")
    private String code; // 小程序端通过wx.login()获取的code

    private String shareToken; //分享code
}