package com.funfun.schedule.controller;

import com.funfun.schedule.controller.financialplan.FinancialPlanPermissionChecker;
import com.funfun.schedule.controller.financialplan.WebTraceContext;
import com.funfun.schedule.dto.financialplan.FinancialPlanDashboardResponse;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.FinancialPlanStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收益统计看板控制器（API-10）。
 *
 * <p>仅编排：权限校验 + 调用 {@link FinancialPlanStatsService} 三类计算结果组装。
 */
@RestController
@RequestMapping("/api/financial-plans")
public class FinancialPlanDashboardController {

    @Autowired
    private FinancialPlanStatsService financialPlanStatsService;

    @Autowired
    private FinancialPlanPermissionChecker permissionChecker;

    /**
     * API-10：获取收益统计看板。
     */
    @GetMapping("/{planId}/dashboard")
    public CommonResponse<FinancialPlanDashboardResponse> getDashboard(@PathVariable Long planId) {
        String traceId = WebTraceContext.newTraceId();
        permissionChecker.loadPlanWithAccess(planId, traceId);

        FinancialPlanDashboardResponse response = new FinancialPlanDashboardResponse();
        response.setPlanSummary(financialPlanStatsService.calcPlanSummary(planId));
        response.setAssetSummaries(financialPlanStatsService.calcAssetSummaries(planId));
        response.setProgress(financialPlanStatsService.calcProgressSnapshot(planId));
        return CommonResponse.success(response);
    }
}
