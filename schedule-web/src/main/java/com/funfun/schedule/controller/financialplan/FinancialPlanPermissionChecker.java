package com.funfun.schedule.controller.financialplan;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.service.FinancialPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 理财计划接口权限校验器。
 *
 * <p>统一封装：
 * <ul>
 *   <li>当前登录用户是否为目标 groupId 的成员（写/读路径前置）。</li>
 *   <li>按 planId 加载计划并校验 groupId 成员关系。</li>
 * </ul>
 * 未授权一律抛 {@code FP_PERMISSION_DENIED}。
 */
@Component
public class FinancialPlanPermissionChecker {

    @Autowired
    private FinancialPlanService financialPlanService;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    /**
     * 校验当前登录用户是否为指定群组成员。
     *
     * @param groupId 群组 ID（必填）
     * @param traceId 当前请求 traceId
     */
    public void ensureGroupMember(Long groupId, String traceId) {
        if (groupId == null) {
            FinancialPlanError.FP_PERMISSION_DENIED.throwsError("traceId=" + traceId + ", reason=missing groupId");
        }
        Long userId = UserContext.getUserId();
        if (userId == null) {
            FinancialPlanError.FP_PERMISSION_DENIED.throwsError("traceId=" + traceId + ", reason=unauthenticated");
        }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            FinancialPlanError.FP_PERMISSION_DENIED.throwsError(
                    "traceId=" + traceId + ", groupId=" + groupId + ", userId=" + userId);
        }
    }

    /**
     * 加载计划并校验当前登录用户的群组成员身份；未授权抛 FP_PERMISSION_DENIED。
     *
     * @param planId  计划主键
     * @param traceId 请求 traceId
     * @return 加载后的计划实体
     */
    public FinancialPlan loadPlanWithAccess(Long planId, String traceId) {
        // 通过 service 加载，未找到时由 service 抛 FP_PLAN_NOT_FOUND
        FinancialPlan plan = financialPlanService.getPlan(planId);
        ensureGroupMember(plan.getGroupId(), traceId);
        return plan;
    }
}
