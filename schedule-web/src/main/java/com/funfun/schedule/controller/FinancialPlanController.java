package com.funfun.schedule.controller;

import com.funfun.schedule.controller.financialplan.FinancialPlanPermissionChecker;
import com.funfun.schedule.controller.financialplan.WebTraceContext;
import com.funfun.schedule.dto.CreateFinancialPlanCommand;
import com.funfun.schedule.dto.FinancialPlanDetailDTO;
import com.funfun.schedule.dto.QueryFinancialPlanCommand;
import com.funfun.schedule.dto.UpdateFinancialPlanCommand;
import com.funfun.schedule.dto.financialplan.ArchiveFinancialPlanRequest;
import com.funfun.schedule.dto.financialplan.ArchiveFinancialPlanResponse;
import com.funfun.schedule.dto.financialplan.CreateFinancialPlanRequest;
import com.funfun.schedule.dto.financialplan.FinancialPlanDetailResponse;
import com.funfun.schedule.dto.financialplan.FinancialPlanIdResponse;
import com.funfun.schedule.dto.financialplan.FinancialPlanListItemDTO;
import com.funfun.schedule.dto.financialplan.FinancialPlanQueryRequest;
import com.funfun.schedule.dto.financialplan.FinancialPlanQueryResponse;
import com.funfun.schedule.dto.financialplan.UpdateFinancialPlanRequest;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.FinancialPlanService;
import com.funfun.schedule.service.FinancialPlanStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * 理财计划核心控制器（API-1 / API-2 / API-3 / API-9 / DELETE 归档）。
 *
 * <p>本控制器仅负责 DTO ↔ Command 转换和权限前置校验，
 * 不承载业务规则；所有业务约束（INV-1 ~ INV-7）落在 service 层。
 */
@RestController
@RequestMapping("/api/financial-plans")
public class FinancialPlanController {

    @Autowired
    private FinancialPlanService financialPlanService;

    @Autowired
    private FinancialPlanStatsService financialPlanStatsService;

    @Autowired
    private FinancialPlanPermissionChecker permissionChecker;

    /**
     * API-1：理财计划分页查询。
     */
    @PostMapping("/query")
    public CommonResponse<FinancialPlanQueryResponse> queryPlans(
            @RequestBody FinancialPlanQueryRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_QUERY_INVALID.throwsError("traceId=" + traceId + ", reason=request body is null");
        }
        permissionChecker.ensureGroupMember(request.getGroupId(), traceId);

        QueryFinancialPlanCommand command = toQueryCommand(request);
        Page<FinancialPlan> page = financialPlanService.queryPlans(command);

        FinancialPlanQueryResponse response = new FinancialPlanQueryResponse();
        response.setList(page.getContent().stream()
                .map(this::toListItem)
                .collect(Collectors.toList()));
        response.setTotal(page.getTotalElements());
        response.setPageNo(page.getNumber() + 1);
        response.setPageSize(page.getSize());
        return CommonResponse.success(response);
    }

    /**
     * API-2：创建理财计划。
     */
    @PostMapping
    public CommonResponse<FinancialPlanIdResponse> createPlan(
            @RequestBody CreateFinancialPlanRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("traceId=" + traceId + ", reason=request body is null");
        }
        permissionChecker.ensureGroupMember(request.getGroupId(), traceId);

        CreateFinancialPlanCommand command = new CreateFinancialPlanCommand();
        command.setGroupId(request.getGroupId());
        command.setOwnerUserId(request.getOwnerUserId());
        command.setPlanName(request.getPlanName());
        command.setPlanType(request.getPlanType());
        command.setStockSubType(request.getStockSubType());
        command.setTimeRangeType(request.getTimeRangeType());
        command.setFiscalYear(request.getFiscalYear());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setRemark(request.getRemark());

        FinancialPlan plan = financialPlanService.createPlan(command);
        return CommonResponse.success(toIdResponse(plan));
    }

    /**
     * API-3：更新理财计划。
     */
    @PutMapping("/{planId}")
    public CommonResponse<FinancialPlanIdResponse> updatePlan(
            @PathVariable Long planId,
            @RequestBody UpdateFinancialPlanRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("traceId=" + traceId + ", reason=request body is null");
        }
        // 权限前置：先按 planId 加载计划并校验群组成员身份
        permissionChecker.loadPlanWithAccess(planId, traceId);

        UpdateFinancialPlanCommand command = new UpdateFinancialPlanCommand();
        command.setPlanName(request.getPlanName());
        command.setStatus(request.getStatus());
        command.setTimeRangeType(request.getTimeRangeType());
        command.setFiscalYear(request.getFiscalYear());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setRemark(request.getRemark());
        command.setVersion(request.getVersion());

        FinancialPlan plan = financialPlanService.updatePlan(planId, command);
        return CommonResponse.success(toIdResponse(plan));
    }

    /**
     * API-9：理财计划详情。
     */
    @GetMapping("/{planId}")
    public CommonResponse<FinancialPlanDetailResponse> getPlanDetail(@PathVariable Long planId) {
        String traceId = WebTraceContext.newTraceId();
        permissionChecker.loadPlanWithAccess(planId, traceId);

        FinancialPlanDetailDTO detail = financialPlanService.loadPlanDetail(planId);
        FinancialPlanDetailResponse response = new FinancialPlanDetailResponse();
        response.setPlan(detail.getPlan());
        response.setAssets(detail.getAssets());
        response.setRealizationBatches(detail.getRealizationBatches());
        response.setSummary(financialPlanStatsService.calcPlanSummary(planId));
        return CommonResponse.success(response);
    }

    /**
     * API-11：停用（归档）理财计划。
     */
    @DeleteMapping("/{planId}")
    public CommonResponse<ArchiveFinancialPlanResponse> archivePlan(
            @PathVariable Long planId,
            @RequestBody ArchiveFinancialPlanRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null || request.getVersion() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("traceId=" + traceId + ", reason=version is required");
        }
        permissionChecker.loadPlanWithAccess(planId, traceId);

        FinancialPlan archived = financialPlanService.archivePlan(planId, request.getVersion());
        ArchiveFinancialPlanResponse response = new ArchiveFinancialPlanResponse();
        response.setPlanId(archived.getPlanId());
        response.setStatus(archived.getStatus());
        return CommonResponse.success(response);
    }

    // ===================== 内部工具方法 =====================

    /** 请求 DTO → service 命令对象（保留所有过滤字段）。 */
    private QueryFinancialPlanCommand toQueryCommand(FinancialPlanQueryRequest request) {
        QueryFinancialPlanCommand command = new QueryFinancialPlanCommand();
        command.setGroupId(request.getGroupId());
        command.setOwnerUserId(request.getOwnerUserId());
        command.setKeyword(request.getKeyword());
        command.setPlanType(request.getPlanType());
        command.setExecutionStatus(request.getExecutionStatus());
        command.setTimeRangeType(request.getTimeRangeType());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setPageNo(request.getPageNo());
        command.setPageSize(request.getPageSize());
        return command;
    }

    /** entity → 列表项 DTO。 */
    private FinancialPlanListItemDTO toListItem(FinancialPlan plan) {
        FinancialPlanListItemDTO item = new FinancialPlanListItemDTO();
        item.setPlanId(plan.getPlanId());
        item.setGroupId(plan.getGroupId());
        item.setOwnerUserId(plan.getOwnerUserId());
        item.setPlanName(plan.getPlanName());
        item.setPlanType(plan.getPlanType());
        item.setStockSubType(plan.getStockSubType());
        item.setStatus(plan.getStatus());
        item.setTimeRangeType(plan.getTimeRangeType());
        item.setFiscalYear(plan.getFiscalYear());
        item.setStartDate(plan.getStartDate());
        item.setEndDate(plan.getEndDate());
        item.setRemark(plan.getRemark());
        item.setVersion(plan.getVersion());
        return item;
    }

    /** entity → planId/status/version 公共响应。 */
    private FinancialPlanIdResponse toIdResponse(FinancialPlan plan) {
        FinancialPlanIdResponse response = new FinancialPlanIdResponse();
        response.setPlanId(plan.getPlanId());
        response.setStatus(plan.getStatus());
        response.setVersion(plan.getVersion());
        return response;
    }
}
