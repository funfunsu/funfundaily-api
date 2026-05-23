package com.funfun.schedule.controller;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.InvitationDTO;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邀请函接口。邀请函以 schedule_item 存储：
 * 发出的邀请 item_type=invSent；收到的邀请 item_type=invRecv（parentId 指向对应 invSent 记录）。
 */
@RestController
@RequestMapping("/api/invitation")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    /**
     * 邀请函主页列表：当前用户「我发出的」（限定群组）+「我收到的」（不限群组）。
     */
    @GetMapping("/list")
    public CommonResponse<List<InvitationDTO>> list(@RequestParam Long groupId) {
        Long userId = UserContext.getUserId();
        return CommonResponse.success(invitationService.listForHome(userId, groupId));
    }

    /**
     * 按 id 查询单条邀请函详情。
     */
    @GetMapping("/{id}")
    public CommonResponse<InvitationDTO> get(@PathVariable Long id) {
        return CommonResponse.success(invitationService.getById(id));
    }

    /**
     * 创建 / 更新「发出的邀请」。更新时若时间/地点变更会级联同步收到记录。
     */
    @PostMapping("/save")
    public CommonResponse<InvitationDTO> save(@RequestBody InvitationDTO request) {
        Long userId = UserContext.getUserId();
        return CommonResponse.success(invitationService.saveSent(userId, request));
    }

    /**
     * 删除「发出的邀请」（仅创建人）。
     */
    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        invitationService.deleteSent(userId, id);
        return CommonResponse.success();
    }

    /**
     * 受邀人「收下邀请」：依据原邀请 id 拷贝当前数据生成一条收到记录。
     */
    @PostMapping("/accept")
    public CommonResponse<InvitationDTO> accept(@RequestBody AcceptInvitationRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            CommonException.LOGIN_INVALID.throwsError("请先登录");
        }
        return CommonResponse.success(
                invitationService.accept(userId, request.getInvitationId(), request.getRecipientName()));
    }

    public static class AcceptInvitationRequest {
        private Long invitationId;
        private String recipientName;

        public Long getInvitationId() { return invitationId; }
        public void setInvitationId(Long invitationId) { this.invitationId = invitationId; }
        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    }
}
