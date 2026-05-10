package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.CreateRealizationBatchCommand;
import com.funfun.schedule.dto.RecordRealizationBuyCommand;
import com.funfun.schedule.dto.RecordRealizationSellCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.OperationType;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.StageStatus;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import com.funfun.schedule.repository.RealizationOperationRepository;
import com.funfun.schedule.service.FinancialPlanRealizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 兑现批次领域服务实现。
 */
@Service
public class FinancialPlanRealizationServiceImpl implements FinancialPlanRealizationService {

    /** BigDecimal 除法保留位数（与持久层 DECIMAL(20,8) 对齐）。 */
    private static final int MONEY_SCALE = 8;

    private final FinancialPlanRepository financialPlanRepository;
    private final FinancialPlanAssetRepository financialPlanAssetRepository;
    private final RealizationBatchRepository realizationBatchRepository;
    private final RealizationOperationRepository realizationOperationRepository;

    @Autowired
    public FinancialPlanRealizationServiceImpl(
            FinancialPlanRepository financialPlanRepository,
            FinancialPlanAssetRepository financialPlanAssetRepository,
            RealizationBatchRepository realizationBatchRepository,
            RealizationOperationRepository realizationOperationRepository) {
        this.financialPlanRepository = financialPlanRepository;
        this.financialPlanAssetRepository = financialPlanAssetRepository;
        this.realizationBatchRepository = realizationBatchRepository;
        this.realizationOperationRepository = realizationOperationRepository;
    }

    /** 为指定标的创建一个兑现批次。 */
    @Override
    @Transactional
    public RealizationBatch createBatch(Long planId, CreateRealizationBatchCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        FinancialPlan plan = loadActivePlan(planId);
        FinancialPlanAsset asset = loadAssetForWrite(command.getAssetId());
        ensureAssetBelongsToPlan(asset, plan.getPlanId());

        BigDecimal newBatchQty = command.getQuantity();
        // INV-3：批次数量必须为正。
        if (newBatchQty == null || newBatchQty.compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "quantity must be positive");
        }
        if (command.getBatchName() == null || command.getBatchName().trim().isEmpty()) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("batchName is required");
        }

        // INV-3：所有有效批次累计数量 + 新批次数量 不得超过 planQuantity。
        // 等价于：未完成批次数量 + 新批次数量 <= planQuantity - realizedQuantity。
        BigDecimal existingTotal = nullSafe(
                realizationBatchRepository.sumBatchQuantityByAssetId(asset.getAssetId()));
        BigDecimal newTotal = existingTotal.add(newBatchQty);
        if (newTotal.compareTo(asset.getPlanQuantity()) > 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "assetId=" + asset.getAssetId()
                            + ", existing=" + existingTotal
                            + ", new=" + newBatchQty
                            + ", planQuantity=" + asset.getPlanQuantity());
        }

        RealizationBatch batch = new RealizationBatch();
        batch.setPlanId(plan.getPlanId());
        batch.setAssetId(asset.getAssetId());
        batch.setBatchName(command.getBatchName().trim());
        batch.setQuantity(newBatchQty);
        batch.setStageStatus(StageStatus.PENDING_BUY);
        batch.setFeeTotal(BigDecimal.ZERO);
        batch.setNote(command.getNote());

        return realizationBatchRepository.save(batch);
    }

    /** 登记一次买入操作并推进批次状态。 */
    @Override
    @Transactional
    public RealizationBatch recordBuy(Long planId, Long batchId, RecordRealizationBuyCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);
        ensureBatchVersionMatches(batch, command.getVersion());

        // 状态校验：仅在 PENDING_BUY 与 PARTIAL_BOUGHT 阶段允许买入。
        StageStatus stage = batch.getStageStatus();
        if (stage == StageStatus.PENDING_SELL || stage == StageStatus.COMPLETED) {
            FinancialPlanError.FP_STAGE_CONFLICT.throwsError(
                    "buy not allowed at stage=" + stage);
        }

        validateOperationCommonFields(
                command.getTradeDate(), command.getActualBuyPrice(), command.getQuantity(), command.getFee());

        // INV-3：累计买入数量不得超过批次数量。
        BigDecimal totalBuyQtyBefore = sumOpQuantity(batchId, OperationType.BUY);
        BigDecimal newTotalBuyQty = totalBuyQtyBefore.add(command.getQuantity());
        if (newTotalBuyQty.compareTo(batch.getQuantity()) > 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "batchId=" + batchId
                            + ", before=" + totalBuyQtyBefore
                            + ", new=" + command.getQuantity()
                            + ", batchQty=" + batch.getQuantity());
        }

        // 写入操作明细。
        RealizationOperation op = buildOperation(
                batchId, OperationType.BUY,
                command.getTradeDate(), command.getActualBuyPrice(),
                command.getQuantity(), command.getFee(), command.getNote());
        realizationOperationRepository.save(op);

        // 更新批次累计金额、加权均价与费用。
        BigDecimal newBuyAmount = nullSafe(batch.getActualBuyAmount())
                .add(command.getActualBuyPrice().multiply(command.getQuantity()));
        batch.setActualBuyAmount(newBuyAmount);
        batch.setActualBuyPrice(safeDivide(newBuyAmount, newTotalBuyQty));
        batch.setBuyTradeDate(command.getTradeDate());
        batch.setFeeTotal(nullSafe(batch.getFeeTotal()).add(nullSafe(command.getFee())));

        // 重算阶段状态。
        BigDecimal totalSellQty = sumOpQuantity(batchId, OperationType.SELL);
        batch.setStageStatus(computeStageStatus(batch.getQuantity(), newTotalBuyQty, totalSellQty));

        return saveBatchWithLock(batch);
    }

    /** 登记一次卖出操作并推进批次状态；卖出累计达到批次数量时进入 COMPLETED。 */
    @Override
    @Transactional
    public RealizationBatch recordSell(Long planId, Long batchId, RecordRealizationSellCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);
        ensureBatchVersionMatches(batch, command.getVersion());

        // INV-4：卖出登记前必须存在买入记录。
        BigDecimal totalBuyQty = sumOpQuantity(batchId, OperationType.BUY);
        if (totalBuyQty.compareTo(BigDecimal.ZERO) <= 0
                || batch.getStageStatus() == StageStatus.PENDING_BUY) {
            FinancialPlanError.FP_SELL_BEFORE_BUY.throwsError("batchId=" + batchId);
        }
        // 状态校验：COMPLETED 阶段不再允许卖出。
        if (batch.getStageStatus() == StageStatus.COMPLETED) {
            FinancialPlanError.FP_STAGE_CONFLICT.throwsError(
                    "sell not allowed at stage=COMPLETED");
        }

        validateOperationCommonFields(
                command.getTradeDate(), command.getActualSellPrice(), command.getQuantity(), command.getFee());

        // INV-3：累计卖出数量不得超过批次数量；同时不得超过累计买入数量。
        BigDecimal totalSellQtyBefore = sumOpQuantity(batchId, OperationType.SELL);
        BigDecimal newTotalSellQty = totalSellQtyBefore.add(command.getQuantity());
        if (newTotalSellQty.compareTo(batch.getQuantity()) > 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "batchId=" + batchId
                            + ", before=" + totalSellQtyBefore
                            + ", new=" + command.getQuantity()
                            + ", batchQty=" + batch.getQuantity());
        }
        if (newTotalSellQty.compareTo(totalBuyQty) > 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "sell exceeds buy: batchId=" + batchId
                            + ", newSell=" + newTotalSellQty
                            + ", buy=" + totalBuyQty);
        }

        // 写入操作明细。
        RealizationOperation op = buildOperation(
                batchId, OperationType.SELL,
                command.getTradeDate(), command.getActualSellPrice(),
                command.getQuantity(), command.getFee(), command.getNote());
        realizationOperationRepository.save(op);

        // 更新批次累计金额、加权均价与费用。
        BigDecimal newSellAmount = nullSafe(batch.getActualSellAmount())
                .add(command.getActualSellPrice().multiply(command.getQuantity()));
        batch.setActualSellAmount(newSellAmount);
        batch.setActualSellPrice(safeDivide(newSellAmount, newTotalSellQty));
        batch.setSellTradeDate(command.getTradeDate());
        batch.setFeeTotal(nullSafe(batch.getFeeTotal()).add(nullSafe(command.getFee())));

        // 重算阶段状态；进入 COMPLETED 时同步统计。
        StageStatus newStage = computeStageStatus(batch.getQuantity(), totalBuyQty, newTotalSellQty);
        StageStatus prevStage = batch.getStageStatus();
        batch.setStageStatus(newStage);

        if (newStage == StageStatus.COMPLETED) {
            // 实际盈利 = 卖出总额 - 买入总额 - 费用合计。
            BigDecimal profit = nullSafe(batch.getActualSellAmount())
                    .subtract(nullSafe(batch.getActualBuyAmount()))
                    .subtract(nullSafe(batch.getFeeTotal()));
            batch.setActualProfit(profit);

            // INV-6：批次完成后同步标的 realizedQuantity。
            // 仅在状态首次进入 COMPLETED 时累加，避免重复累计。
            if (prevStage != StageStatus.COMPLETED) {
                syncAssetRealizedQuantity(batch.getAssetId(), batch.getQuantity());
            }
        }

        return saveBatchWithLock(batch);
    }

    // ===================== 内部工具方法 =====================

    /** 加载计划并校验是否仍可编辑（未归档），违反 INV-7 抛 FP_PLAN_ALREADY_ARCHIVED。 */
    private FinancialPlan loadActivePlan(Long planId) {
        if (planId == null) {
            FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId is null");
        }
        FinancialPlan plan = financialPlanRepository
                .findByPlanIdAndDeletedFalse(planId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId=" + planId);
                    return null;
                });
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            FinancialPlanError.FP_PLAN_ALREADY_ARCHIVED.throwsError("planId=" + planId);
        }
        return plan;
    }

    /** 加载标的，未找到时抛 FP_ASSET_NOT_FOUND。 */
    private FinancialPlanAsset loadAssetForWrite(Long assetId) {
        if (assetId == null) {
            FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError("assetId is null");
        }
        return financialPlanAssetRepository
                .findByAssetIdAndDeletedFalse(assetId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError("assetId=" + assetId);
                    return null;
                });
    }

    /** 加载批次，未找到时抛 FP_BATCH_NOT_FOUND。 */
    private RealizationBatch loadBatchForWrite(Long batchId) {
        if (batchId == null) {
            FinancialPlanError.FP_BATCH_NOT_FOUND.throwsError("batchId is null");
        }
        return realizationBatchRepository
                .findByBatchIdAndDeletedFalse(batchId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_BATCH_NOT_FOUND.throwsError("batchId=" + batchId);
                    return null;
                });
    }

    /** 校验标的所属计划与传入 planId 一致。 */
    private void ensureAssetBelongsToPlan(FinancialPlanAsset asset, Long planId) {
        if (!Objects.equals(asset.getPlanId(), planId)) {
            FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError(
                    "assetId=" + asset.getAssetId() + ", planId=" + planId);
        }
    }

    /** 校验批次所属计划与传入 planId 一致。 */
    private void ensureBatchBelongsToPlan(RealizationBatch batch, Long planId) {
        if (!Objects.equals(batch.getPlanId(), planId)) {
            FinancialPlanError.FP_BATCH_NOT_FOUND.throwsError(
                    "batchId=" + batch.getBatchId() + ", planId=" + planId);
        }
    }

    /** 校验客户端版本号与数据库当前版本号一致。 */
    private void ensureBatchVersionMatches(RealizationBatch batch, Integer expectedVersion) {
        if (expectedVersion == null) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("missing version");
        }
        if (!Objects.equals(batch.getVersion(), expectedVersion)) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                    "expected=" + expectedVersion + ", actual=" + batch.getVersion());
        }
    }

    /** 校验操作必填字段（数量/价格为正、费用非负、日期非空）。 */
    private void validateOperationCommonFields(java.time.LocalDate tradeDate,
                                               BigDecimal price,
                                               BigDecimal quantity,
                                               BigDecimal fee) {
        if (tradeDate == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("tradeDate is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("price must be positive");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "operation quantity must be positive");
        }
        if (fee != null && fee.compareTo(BigDecimal.ZERO) < 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("fee must be non-negative");
        }
    }

    /** 计算批次操作流水的累计数量，结果非空。 */
    private BigDecimal sumOpQuantity(Long batchId, OperationType opType) {
        List<RealizationOperation> ops = realizationOperationRepository
                .findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(batchId, opType);
        BigDecimal sum = BigDecimal.ZERO;
        for (RealizationOperation o : ops) {
            sum = sum.add(nullSafe(o.getQuantity()));
        }
        return sum;
    }

    /**
     * 根据累计买入/卖出数量重算批次阶段状态：
     * <ul>
     *   <li>买卖均达到 batchQty：COMPLETED；</li>
     *   <li>买入达到 batchQty：PENDING_SELL；</li>
     *   <li>买入大于 0 但未达到 batchQty：PARTIAL_BOUGHT；</li>
     *   <li>买入为 0：PENDING_BUY。</li>
     * </ul>
     */
    private StageStatus computeStageStatus(BigDecimal batchQty,
                                           BigDecimal totalBuyQty,
                                           BigDecimal totalSellQty) {
        if (totalBuyQty.compareTo(batchQty) >= 0
                && totalSellQty.compareTo(batchQty) >= 0) {
            return StageStatus.COMPLETED;
        }
        if (totalBuyQty.compareTo(batchQty) >= 0) {
            return StageStatus.PENDING_SELL;
        }
        if (totalBuyQty.compareTo(BigDecimal.ZERO) > 0) {
            return StageStatus.PARTIAL_BOUGHT;
        }
        return StageStatus.PENDING_BUY;
    }

    /** 构造操作流水实体。 */
    private RealizationOperation buildOperation(Long batchId,
                                                OperationType opType,
                                                java.time.LocalDate tradeDate,
                                                BigDecimal price,
                                                BigDecimal quantity,
                                                BigDecimal fee,
                                                String note) {
        RealizationOperation op = new RealizationOperation();
        op.setBatchId(batchId);
        op.setOperationType(opType);
        op.setTradeDate(tradeDate);
        op.setPrice(price);
        op.setQuantity(quantity);
        op.setFee(nullSafe(fee));
        op.setNote(note);
        return op;
    }

    /** 同步标的的 realizedQuantity（INV-6）。 */
    private void syncAssetRealizedQuantity(Long assetId, BigDecimal completedQty) {
        FinancialPlanAsset asset = loadAssetForWrite(assetId);
        asset.setRealizedQuantity(nullSafe(asset.getRealizedQuantity()).add(completedQty));
        try {
            financialPlanAssetRepository.save(asset);
        } catch (OptimisticLockingFailureException ex) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("assetId=" + assetId);
        }
    }

    /** 保存批次并统一封装乐观锁冲突。 */
    private RealizationBatch saveBatchWithLock(RealizationBatch batch) {
        try {
            return realizationBatchRepository.save(batch);
        } catch (OptimisticLockingFailureException ex) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("batchId=" + batch.getBatchId());
            return null;
        }
    }

    /** 安全除法，遵循 8 位小数 + HALF_UP 舍入。 */
    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** BigDecimal 空值兜底为 0。 */
    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
