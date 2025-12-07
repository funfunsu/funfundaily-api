package com.funfun.schedule.controller;

import com.funfun.schedule.anno.NoAuth;
import com.funfun.schedule.dto.LoginRequest;
import com.funfun.schedule.entity.ShareRecord;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.ShareService;
import com.funfun.schedule.service.UserService;
import com.funfun.schedule.service.WeChatMiniService;
import com.funfun.schedule.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
//@Slf4j
public class LoginController {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LoginController.class); // 手动添加
    private final WeChatMiniService weChatMiniService; // 注入接口（而非具体实现）
    private final JwtUtil jwtUtil;
    private final UserService userService; // 注入用户服务（新增）
    private final ShareService shareService; // 分享（新增）

    /**
     * curl -X POST http://localhost:8080/login \
     *   -H "Content-Type: application/json" \
     *   -d '{"code": "your_mini_program_login_code"}'
     * @param loginRequest
     * @return
     */
    @NoAuth
    @PostMapping("/login")
    public CommonResponse<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("小程序登录请求，code：{}", loginRequest.getCode());
        String openId = weChatMiniService.jscode2session(loginRequest.getCode()) ;
        Long invitorId = null;
        if (loginRequest.getShareToken() != null && !loginRequest.getShareToken().isBlank()){
            Optional<ShareRecord> shareRecordOptional = shareService.getShareByToken(loginRequest.getShareToken());
            if (shareRecordOptional.isPresent()){
                invitorId = shareRecordOptional.get().getCreatorId();
            }
        }
        Long localUserId = userService.getOrCreateUserIdByOpenId(openId,invitorId);
        String token = jwtUtil.generateToken(String.valueOf(localUserId));
        log.info("登录成功：本地userId={}, Token={},invitorId:{}", localUserId, token,invitorId);
        return CommonResponse.success(token);
    }
}