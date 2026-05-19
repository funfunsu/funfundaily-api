package com.funfun.schedule.controller;

import com.funfun.schedule.controller.financialplan.FinancialPlanPermissionChecker;
import com.funfun.schedule.controller.financialplan.WebTraceContext;
import com.funfun.schedule.dto.CreateRealizationBatchCommand;
import com.funfun.schedule.dto.RecordRealizationBuyCommand;
import com.funfun.schedule.dto.RecordRealizationSellCommand;
import com.funfun.schedule.dto.UpdateRealizationBatchCommand;
import com.funfun.schedule.dto.financialplan.CreateRealizationBatchRequest;
import com.funfun.schedule.dto.financialplan.CreateRealizationBatchResponse;
import com.funfun.schedule.dto.financialplan.RecordRealizationBuyRequest;
import com.funfun.schedule.dto.financialplan.RecordRealizationBuyResponse;
import com.funfun.schedule.dto.financialplan.RecordRealizationSellRequest;
import com.funfun.schedule.dto.financialplan.RecordRealizationSellResponse;
import com.funfun.schedule.dto.financialplan.UpdateRealizationBatchRequest;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.repository.RealizationBatchRepository;
import com.funfun.schedule.repository.RealizationOperationRepository;
import com.funfun.schedule.service.FinancialPlanRealizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 兑现批次控制器（API-6 / API-7 / API-8 + 操作明细查询）。
 *
 * <p>新模型：买入/卖出可多次；不再要求批次 version。
 */
@RestController
@RequestMapping("/api/financial-plans")
public class FinancialPlanRealizationController {

    @Autowired
    private FinancialPlanRealizationService realizationService;

    @Autowired
    private FinancialPlanPermissionChecker permissionChecker;

    @Autowired
    private RealizationBatchRepository batchRepository;

    @Autowired
    private RealizationOperationRepository operationRepository;

    /**
     * API-6：创建兑现批次。
     */
    @PostMapping("/{planId}/realizations")
    public CommonResponse<CreateRealizationBatchResponse> createBatch(
            @PathVariable Long planId,
            @RequestBody CreateRealizationBatchRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=request body is null");
        }
        if (request.getAssetId() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=assetId is required");
        }
        permissionChecker.loadPlanWithAccess(planId, traceId);

        CreateRealizationBatchCommand command = new CreateRealizationBatchCommand();
        command.setAssetId(request.getAssetId());
        command.setBatchType(request.getBatchType());
        command.setDirection(request.getDirection());
        command.setBatchName(request.getBatchName());
        command.setQuantity(request.getQuantity());
        command.setPlanBuyPrice(request.getPlanBuyPrice());
        command.setPlanSellPrice(request.getPlanSellPrice());
        command.setExpirationDate(request.getExpirationDate());
        command.setNote(request.getNote());

        RealizationBatch batch = realizationService.createBatch(planId, command);

        CreateRealizationBatchResponse response = new CreateRealizationBatchResponse();
        response.setBatchId(batch.getBatchId());
        response.setPlanId(batch.getPlanId());
        response.setAssetId(batch.getAssetId());
        response.setQuantity(batch.getQuantity());
        response.setStageStatus(batch.getStageStatus());
        return CommonResponse.success(response);
    }

    /**
     * API-7：登记兑现买入（可多次）。
     */
    @PostMapping("/{planId}/realizations/{batchId}/buy")
    public CommonResponse<RecordRealizationBuyResponse> recordBuy(
            @PathVariable Long planId,
            @PathVariable Long batchId,
            @RequestBody RecordRealizationBuyRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=request body is null");
        }
        permissionChecker.loadPlanWithAccess(planId, traceId);

        RecordRealizationBuyCommand command = new RecordRealizationBuyCommand();
        command.setTradeDate(request.getTradeDate());
        command.setActualBuyPrice(request.getActualBuyPrice());
        command.setQuantity(request.getQuantity());
        command.setFee(request.getFee());
        command.setNote(request.getNote());

        RealizationBatch batch = realizationService.recordBuy(planId, batchId, command);

        RecordRealizationBuyResponse response = new RecordRealizationBuyResponse();
        response.setBatchId(batch.getBatchId());
        response.setStageStatus(batch.getStageStatus());
        response.setActualBuyAmount(batch.getActualBuyAmount());
        return CommonResponse.success(response);
    }

    /**
     * API-8：登记兑现卖出（可多次）。
     */
    @PostMapping("/{planId}/realizations/{batchId}/sell")
    public CommonResponse<RecordRealizationSellResponse> recordSell(
            @PathVariable Long planId,
            @PathVariable Long batchId,
            @RequestBody RecordRealizationSellRequest request) {
        String traceId = WebTraceContext.newTraceId();
        if (request == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "traceId=" + traceId + ", reason=request body is null");
        }
        permissionChecker.loadPlanWithAccess(planId, traceId);

        RecordRealizationSellCommand command = new RecordRealizationSellCommand();
        command.setTradeDate(request.getTradeDate());
        command.setActualSellPrice(request.getActualSellPrice());
        command.setQuantity(request.getQuantity());
        command.setFee(request.getFee());
        command.setNote(request.getNote());

        RealizationBatch batch = realizationService.recordSell(planId, batchId, command);

        RecordRealizationSellResponse response = new RecordRealizationSellResponse();
        response.setBatchId(batch.getBatchId());
        response.setStageStatus(batch.getStageStatus());
        response.setActualProfit(batch.getActualProfit());
        return CommonResponse.success(response);
    }

    /**
     * 编辑兑现批次：调整名称 / 数量 / 计划价 / 方向 / 到期日 / 备注（batchType 不可变更）。
     */
    @PutMapping("/{planId}/realizations/{batchId}")
    public CommonResponse<RealizationBatch> updateBatch(
            @PathVariable Long planId,
            @PathVariable Long batchId,
            @RequestBody UpdateRealizationBatchRequest request) {
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

        UpdateRealizationBatchCommand command = new UpdateRealizationBatchCommand();
        command.setBatchName(request.getBatchName());
        command.setDirection(request.getDirection());
        command.setQuantity(request.getQuantity());
        command.setPlanBuyPrice(request.getPlanBuyPrice());
        command.setPlanSellPrice(request.getPlanSellPrice());
        command.setExpirationDate(request.getExpirationDate());
        command.setNote(request.getNote());
        command.setVersion(request.getVersion());

        RealizationBatch batch = realizationService.updateBatch(planId, batchId, command);
        return CommonResponse.success(batch);
    }

    /**
     * 列出指定批次的操作明细（按时间正序），供前端在批次行下展开。
     */
    @GetMapping("/{planId}/realizations/{batchId}/operations")
    public CommonResponse<List<RealizationOperation>> listOperations(
            @PathVariable Long planId,
            @PathVariable Long batchId) {
        String traceId = WebTraceContext.newTraceId();
        permissionChecker.loadPlanWithAccess(planId, traceId);

        RealizationBatch batch = batchRepository.findByBatchIdAndDeletedFalse(batchId)
                .orElse(null);
        if (batch == null || !batch.getPlanId().equals(planId)) {
            FinancialPlanError.FP_BATCH_NOT_FOUND.throwsError(
                    "traceId=" + traceId + ", batchId=" + batchId);
        }
        List<RealizationOperation> ops = operationRepository
                .findByBatchIdOrderByTradeDateAscCreatedAtAsc(batchId);
        return CommonResponse.success(ops);
    }
}
