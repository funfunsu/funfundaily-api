package com.funfun.schedule.controller;

import com.funfun.schedule.controller.financialplan.FinancialPlanPermissionChecker;
import com.funfun.schedule.controller.financialplan.WebTraceContext;
import com.funfun.schedule.dto.SaveFinancialPlanAssetItem;
import com.funfun.schedule.dto.UpdateFinancialPlanAssetParamsCommand;
import com.funfun.schedule.dto.financialplan.SaveFinancialPlanAssetsRequest;
import com.funfun.schedule.dto.financialplan.SaveFinancialPlanAssetsResponse;
import com.funfun.schedule.dto.financialplan.UpdateFinancialPlanAssetParamsRequest;
import com.funfun.schedule.dto.financialplan.UpdateFinancialPlanAssetParamsResponse;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.PlanAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 计划标的控制器（API-4 / API-5）。
 *
 * <p>新模型：仅 stockName / market / targetProfit 三字段。
 */
@RestController
@RequestMapping("/api/financial-plans")
public class FinancialPlanAssetController {

    @Autowired
    private PlanAssetService planAssetService;

    @Autowired
    private FinancialPlanPermissionChecker permissionChecker;

    /**
     * API-4：批量保存计划标的。
     */
    @PostMapping("/{planId}/assets/save")
    public CommonResponse<SaveFinancialPlanAssetsResponse> saveAssets(
            @PathVariable Long planId,
            @RequestBody SaveFinancialPlanAssetsRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=items is empty");
        }
        permissionChecker.loadPlanWithAccess(planId, traceId);

        List<SaveFinancialPlanAssetItem> items = request.getItems();
        List<FinancialPlanAsset> saved = planAssetService.saveAssets(planId, items);

        SaveFinancialPlanAssetsResponse response = new SaveFinancialPlanAssetsResponse();
        response.setPlanId(planId);
        response.setItems(saved);
        return CommonResponse.success(response);
    }

    /**
     * API-5：调整计划标的参数（股票名 / 市场 / 目标利润）。
     */
    @PutMapping("/{planId}/assets/{assetId}")
    public CommonResponse<UpdateFinancialPlanAssetParamsResponse> updateAssetParams(
            @PathVariable Long planId,
            @PathVariable Long assetId,
            @RequestBody UpdateFinancialPlanAssetParamsRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=request body is null");
        }
        if (request.getVersion() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=version is required");
        }
        permissionChecker.loadPlanWithAccess(planId, traceId);

        UpdateFinancialPlanAssetParamsCommand command = new UpdateFinancialPlanAssetParamsCommand();
        command.setStockName(request.getStockName());
        command.setMarket(request.getMarket());
        command.setTargetProfit(request.getTargetProfit());
        command.setVersion(request.getVersion());

        FinancialPlanAsset asset = planAssetService.updateAssetParams(planId, assetId, command);
        return CommonResponse.success(toAssetParamsResponse(asset));
    }

    /** entity → 标的参数响应。 */
    private UpdateFinancialPlanAssetParamsResponse toAssetParamsResponse(FinancialPlanAsset asset) {
        UpdateFinancialPlanAssetParamsResponse response = new UpdateFinancialPlanAssetParamsResponse();
        response.setAssetId(asset.getAssetId());
        response.setStockName(asset.getStockName());
        response.setMarket(asset.getMarket());
        response.setTargetProfit(asset.getTargetProfit());
        response.setVersion(asset.getVersion());
        return response;
    }
}
