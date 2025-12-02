package com.funfun.schedule.controller;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.config.WeChatConfig;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.UpdateUserProfileRequest;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.User;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.repository.UserRepository;
import com.funfun.schedule.service.SessionKeyService;
import com.funfun.schedule.util.WeChatDecryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import javax.validation.Valid;
import java.util.Optional;

/**
 * 用户相关接口（WebFlux 兼容版，移除 Servlet 依赖）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final SessionKeyService sessionKeyService;
    private final WeChatDecryptUtil weChatDecryptUtil;
    private final WeChatConfig weChatConfig;

    /**
     */
    @PostMapping("/update-profile")
    public CommonResponse<Void> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            HttpServletRequest serverHttpRequest) { // WebFlux 原生请求对象，无需额外依赖

        // 3. 查询本地用户，获取 openId
        Optional<User> userOpt = userRepository.findById(UserContext.getUserId());
        if (!userOpt.isPresent()) {
            CommonException.USER_NOT_EXIST.throwsError("");
        }
        User user = userOpt.get();
        String openId = user.getOpenid();

        // 4. 从 MySQL 查询有效的 sessionKey（逻辑不变）
        String sessionKey = sessionKeyService.getValidSessionKey(weChatConfig.getAppid(),openId);
        if (sessionKey == null) {
            CommonException.LOGIN_INVALID.throwsError("sessionKey无效");
        }

        // 5. 解密 + 更新用户资料（逻辑不变）
        JSONObject userInfoJson = weChatDecryptUtil.decrypt(
                request.getEncryptedData(),
                sessionKey,
                request.getIv()
        );
        log.info("解密后的用户信息：{}", userInfoJson);

        String nickname = userInfoJson.getString("nickName");
        String avatarUrl = userInfoJson.getString("avatarUrl");
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("用户资料更新成功：userId={}, nickname={}", UserContext.getUserId(), nickname);
        return CommonResponse.success();
    }

    @GetMapping("/info")
    public CommonResponse<UserInfoDTO> getUserInfo() {
        // 1. 从请求头获取 Token 并校验
        Long localUserId = UserContext.getUserId();

        // 3. 查询本地用户表（MySQL）
        Optional<User> userOpt = userRepository.findById(localUserId);
        if (!userOpt.isPresent()) {
            CommonException.USER_NOT_EXIST.throwsError("");
        }
        User user = userOpt.get();

        // 4. 实体类转换为 DTO（隐藏敏感字段，按需返回）
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        userInfoDTO.setId(user.getId()); // 本地 userId
        userInfoDTO.setNickname(user.getNickname() != null ? user.getNickname() : "微信用户"); // 默认昵称
        userInfoDTO.setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "默认头像URL"); // 默认头像
        userInfoDTO.setCreateTime(user.getRegisterTime()); // 账号创建时间

        log.info("查询用户信息成功：userId={}", localUserId);
        return CommonResponse.success(userInfoDTO);
    }


    // 管理员查询他人信息（示例）
    @GetMapping("/info/{targetUserId}")
    public CommonResponse<UserInfoDTO> getOtherUserInfo(
            @PathVariable Long targetUserId) {
        Long localUserId = UserContext.getUserId();
        //todo 校验用户用是否在群里

        // 3. 查询本地用户表（MySQL）
        Optional<User> userOpt = userRepository.findById(targetUserId);
        if (!userOpt.isPresent()) {
            CommonException.USER_NOT_EXIST.throwsError("");
        }
        User user = userOpt.get();

        // 4. 实体类转换为 DTO（隐藏敏感字段，按需返回）
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        userInfoDTO.setId(user.getId()); // 本地 userId
        userInfoDTO.setNickname(user.getNickname() != null ? user.getNickname() : "微信用户"); // 默认昵称
        userInfoDTO.setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "默认头像URL"); // 默认头像
        userInfoDTO.setCreateTime(user.getRegisterTime()); // 账号创建时间

        log.info("查询用户信息成功：userId={}", localUserId);
        return CommonResponse.success(userInfoDTO);
    }
}