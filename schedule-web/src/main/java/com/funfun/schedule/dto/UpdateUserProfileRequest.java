package com.funfun.schedule.dto;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 更新用户资料请求 DTO（接收小程序加密信息）
 */
@Data
public class UpdateUserProfileRequest {
    @NotBlank(message = "encryptedData 不能为空")
    private String encryptedData; // 加密的用户信息

    @NotBlank(message = "iv 不能为空")
    private String iv; // 加密向量

    @NotBlank(message = "rawData 不能为空")
    private String rawData; // 原始用户信息（可选校验用）

    @NotBlank(message = "signature 不能为空")
    private String signature; // 签名（可选校验用）

    private JSONObject userInfo;

    private String errMsg;
}