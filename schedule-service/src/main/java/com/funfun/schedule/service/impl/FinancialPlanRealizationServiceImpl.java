package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.CreateRealizationBatchCommand;
import com.funfun.schedule.dto.RecordRealizationBuyCommand;
import com.funfun.schedule.dto.RecordRealizationSellCommand;
import com.funfun.schedule.dto.UpdateRealizationBatchCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.BatchDirection;
import com.funfun.schedule.enums.BatchType;
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
 * 兑现批次领域服务实现（新模型）。
 *
 * <p>主要变化：
 * <ul>
 *   <li>批次自带 batchType / direction / expirationDate / planBuyPrice / planSellPrice / quantity；
 *       asset 不再承载这些字段。</li>
 *   <li>买入 / 卖出 不再对批次做乐观锁校验——同一批次可多次买入、多次卖出，
 *       每次操作只追加 RealizationOperation 并刷新批次上的聚合字段。</li>
 *   <li>仍保留 batch.actualBuyAmount / actualSellAmount / actualProfit / stageStatus
 *       作为「持久化的聚合视图」，方便看板与列表展示。</li>
 * </ul>
 */
@Service
public class FinancialPlanRealizationServiceImpl implements FinancialPlanRealizationService {

    /** BigDecimal 除法保留位数。 */
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

        validateBatchCommand(command);

        RealizationBatch batch = new RealizationBatch();
        batch.setPlanId(plan.getPlanId());
        batch.setAssetId(asset.getAssetId());
        batch.setBatchType(command.getBatchType());
        batch.setDirection(command.getBatchType() == BatchType.DERIVATIVE ? command.getDirection() : null);
        batch.setExpirationDate(command.getBatchType() == BatchType.DERIVATIVE ? command.getExpirationDate() : null);
        batch.setBatchName(resolveBatchName(asset, command));
        batch.setQuantity(command.getQuantity());
        batch.setPlanBuyPrice(command.getPlanBuyPrice());
        batch.setPlanSellPrice(command.getPlanSellPrice());
        batch.setStageStatus(StageStatus.PENDING_BUY);
        batch.setFeeTotal(BigDecimal.ZERO);
        batch.setNote(command.getNote());

        return realizationBatchRepository.save(batch);
    }

    /** 登记一次买入操作（可多次；不限定批次状态：COMPLETED 后再买也允许）。 */
    @Override
    @Transactional
    public RealizationBatch recordBuy(Long planId, Long batchId, RecordRealizationBuyCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);

        validateOperationCommonFields(
                command.getTradeDate(), command.getActualBuyPrice(), command.getQuantity(), command.getFee());

        BigDecimal totalBuyQtyBefore = sumOpQuantity(batchId, OperationType.BUY);
        BigDecimal newTotalBuyQty = totalBuyQtyBefore.add(command.getQuantity());
        // batch.quantity 仅作为参考，不再用作买入数量的上限。

        RealizationOperation op = buildOperation(
                batchId, OperationType.BUY,
                command.getTradeDate(), command.getActualBuyPrice(),
                command.getQuantity(), command.getFee(), command.getNote());
        realizationOperationRepository.save(op);

        BigDecimal newBuyAmount = nullSafe(batch.getActualBuyAmount())
                .add(command.getActualBuyPrice().multiply(command.getQuantity()));
        batch.setActualBuyAmount(newBuyAmount);
        batch.setActualBuyPrice(safeDivide(newBuyAmount, newTotalBuyQty));
        batch.setBuyTradeDate(command.getTradeDate());
        batch.setFeeTotal(nullSafe(batch.getFeeTotal()).add(nullSafe(command.getFee())));

        BigDecimal totalSellQty = sumOpQuantity(batchId, OperationType.SELL);
        batch.setStageStatus(computeStageStatus(newTotalBuyQty, totalSellQty));
        // 已实现盈利只在卖出时变化；买入不动 actualProfit，避免再买入后回头把已实现金额拖成负数。

        return saveBatchSafely(batch);
    }

    /** 登记一次卖出操作（可多次）。 */
    @Override
    @Transactional
    public RealizationBatch recordSell(Long planId, Long batchId, RecordRealizationSellCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);

        BigDecimal totalBuyQty = sumOpQuantity(batchId, OperationType.BUY);
        if (totalBuyQty.compareTo(BigDecimal.ZERO) <= 0
                || batch.getStageStatus() == StageStatus.PENDING_BUY) {
            FinancialPlanError.FP_SELL_BEFORE_BUY.throwsError("batchId=" + batchId);
        }
        if (batch.getStageStatus() == StageStatus.COMPLETED) {
            FinancialPlanError.FP_STAGE_CONFLICT.throwsError("sell not allowed at stage=COMPLETED");
        }

        validateOperationCommonFields(
                command.getTradeDate(), command.getActualSellPrice(), command.getQuantity(), command.getFee());

        BigDecimal totalSellQtyBefore = sumOpQuantity(batchId, OperationType.SELL);
        BigDecimal newTotalSellQty = totalSellQtyBefore.add(command.getQuantity());
        // batch.quantity 不再约束卖出上限；仅保留「卖出累计不得超过买入累计」。
        if (newTotalSellQty.compareTo(totalBuyQty) > 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError(
                    "sell exceeds buy: batchId=" + batchId
                            + ", newSell=" + newTotalSellQty
                            + ", buy=" + totalBuyQty);
        }

        RealizationOperation op = buildOperation(
                batchId, OperationType.SELL,
                command.getTradeDate(), command.getActualSellPrice(),
                command.getQuantity(), command.getFee(), command.getNote());
        realizationOperationRepository.save(op);

        BigDecimal newSellAmount = nullSafe(batch.getActualSellAmount())
                .add(command.getActualSellPrice().multiply(command.getQuantity()));
        batch.setActualSellAmount(newSellAmount);
        batch.setActualSellPrice(safeDivide(newSellAmount, newTotalSellQty));
        batch.setSellTradeDate(command.getTradeDate());
        batch.setFeeTotal(nullSafe(batch.getFeeTotal()).add(nullSafe(command.getFee())));

        batch.setStageStatus(computeStageStatus(totalBuyQty, newTotalSellQty));
        recomputeAndStampProfit(batch);

        return saveBatchSafely(batch);
    }

    /**
     * 编辑批次的计划字段；batchType 不可变更，数量不得小于已登记累计买入/卖出数量。
     */
    @Override
    @Transactional
    public RealizationBatch updateBatch(Long planId, Long batchId, UpdateRealizationBatchCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);
        ensureBatchVersionMatches(batch, command.getVersion());

        // 价格：仅校验存在且 ≠ 0（DERIVATIVE 卖空场景允许负数）；EQUITY 仍要求 > 0。
        if (command.getPlanBuyPrice() != null) {
            validateBatchPrice("planBuyPrice", command.getPlanBuyPrice(), batch.getBatchType());
            batch.setPlanBuyPrice(command.getPlanBuyPrice());
        }
        if (command.getPlanSellPrice() != null) {
            validateBatchPrice("planSellPrice", command.getPlanSellPrice(), batch.getBatchType());
            batch.setPlanSellPrice(command.getPlanSellPrice());
        }

        // 数量仅作参考：必须 > 0；不再校验 ≥ 已登记累计买/卖。
        if (command.getQuantity() != null) {
            if (command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError("quantity must be positive");
            }
            batch.setQuantity(command.getQuantity());
        }

        // 名称 / 备注 / 衍生品方向 / 到期日：按字段是否提供来增量更新。
        if (command.getBatchName() != null) {
            String trimmed = command.getBatchName().trim();
            batch.setBatchName(trimmed.isEmpty() ? null : trimmed);
        }
        if (command.getNote() != null) {
            batch.setNote(command.getNote());
        }
        if (batch.getBatchType() == BatchType.DERIVATIVE) {
            if (command.getDirection() != null) {
                batch.setDirection(command.getDirection());
            }
            if (command.getExpirationDate() != null) {
                batch.setExpirationDate(command.getExpirationDate());
            }
        }

        return saveBatchSafely(batch);
    }

    /**
     * 批次价格校验：DERIVATIVE 允许负数（卖空时买入/卖出权利金可能为负）；其余必须严格大于 0。
     */
    private void validateBatchPrice(String fieldName, BigDecimal price, BatchType batchType) {
        if (price == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(fieldName + " is required");
        }
        if (price.compareTo(BigDecimal.ZERO) == 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(fieldName + " must not be zero");
        }
        if (batchType != BatchType.DERIVATIVE && price.compareTo(BigDecimal.ZERO) < 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(fieldName + " must be positive");
        }
    }

    /** 客户端 version 与数据库 version 不一致时抛 FP_VERSION_CONFLICT。 */
    private void ensureBatchVersionMatches(RealizationBatch batch, Integer expectedVersion) {
        if (expectedVersion == null) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("missing version");
        }
        if (!Objects.equals(batch.getVersion(), expectedVersion)) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                    "expected=" + expectedVersion + ", actual=" + batch.getVersion());
        }
    }

    // ===================== 内部工具 =====================

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

    private void ensureAssetBelongsToPlan(FinancialPlanAsset asset, Long planId) {
        if (!Objects.equals(asset.getPlanId(), planId)) {
            FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError(
                    "assetId=" + asset.getAssetId() + ", planId=" + planId);
        }
    }

    private void ensureBatchBelongsToPlan(RealizationBatch batch, Long planId) {
        if (!Objects.equals(batch.getPlanId(), planId)) {
            FinancialPlanError.FP_BATCH_NOT_FOUND.throwsError(
                    "batchId=" + batch.getBatchId() + ", planId=" + planId);
        }
    }

    /** 校验 createBatch 入参；EQUITY 不使用 direction / expirationDate，DERIVATIVE 两者必填。 */
    private void validateBatchCommand(CreateRealizationBatchCommand command) {
        if (command.getBatchType() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("batchType is required");
        }
        if (command.getQuantity() == null || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError("quantity must be positive");
        }
        // DERIVATIVE 批次允许负数价格（卖空场景）；EQUITY 仍要求 > 0。
        validateBatchPrice("planBuyPrice", command.getPlanBuyPrice(), command.getBatchType());
        validateBatchPrice("planSellPrice", command.getPlanSellPrice(), command.getBatchType());
        if (command.getBatchType() == BatchType.DERIVATIVE) {
            if (command.getDirection() == null) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("direction is required for DERIVATIVE batch");
            }
            if (command.getExpirationDate() == null) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("expirationDate is required for DERIVATIVE batch");
            }
        }
    }

    /** 默认批次名：未填则按 「{股票名}-{批次类型/方向}」 拼一个，便于列表辨识。 */
    private String resolveBatchName(FinancialPlanAsset asset, CreateRealizationBatchCommand command) {
        if (command.getBatchName() != null && !command.getBatchName().trim().isEmpty()) {
            return command.getBatchName().trim();
        }
        String suffix = command.getBatchType() == BatchType.DERIVATIVE
                ? "-" + (command.getDirection() != null ? command.getDirection().name() : "DERIVATIVE")
                : "-正股";
        return (asset.getStockName() == null ? "批次" : asset.getStockName()) + suffix;
    }

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
     * 用加权平均成本（WAC）法按操作时间顺序重算累计已实现盈利，并写回 batch.actualProfit。
     *
     * <p>核心思路：
     * <ul>
     *   <li>买入：累加到「持仓数量 / 持仓成本」（buy fee 视为沉没成本，不进 realized）。</li>
     *   <li>卖出：按「当前持仓平均成本」结算这一笔的盈亏 = (sellPrice − avgCost) × sellQty − sellFee；
     *       同时按比例减少持仓数量与成本。</li>
     * </ul>
     * 这样后续的「再买入」不会回头改动已经实现过的盈亏。
     */
    private void recomputeAndStampProfit(RealizationBatch batch) {
        List<RealizationOperation> ops = realizationOperationRepository
                .findByBatchIdOrderByTradeDateAscCreatedAtAsc(batch.getBatchId());

        BigDecimal openQty = BigDecimal.ZERO;
        BigDecimal openCost = BigDecimal.ZERO;
        BigDecimal realized = BigDecimal.ZERO;

        for (RealizationOperation op : ops) {
            BigDecimal opQty = nullSafe(op.getQuantity());
            BigDecimal opPrice = nullSafe(op.getPrice());
            BigDecimal opFee = nullSafe(op.getFee());

            if (op.getOperationType() == OperationType.BUY) {
                openCost = openCost.add(opPrice.multiply(opQty));
                openQty = openQty.add(opQty);
                // 买入手续费不进 realized，留作隐性成本。
            } else {
                BigDecimal avgCost = openQty.signum() == 0
                        ? BigDecimal.ZERO
                        : openCost.divide(openQty, MONEY_SCALE, RoundingMode.HALF_UP);
                BigDecimal thisSellProfit = opPrice.subtract(avgCost)
                        .multiply(opQty)
                        .subtract(opFee);
                realized = realized.add(thisSellProfit);

                BigDecimal consumedCost = avgCost.multiply(opQty);
                openCost = openCost.subtract(consumedCost);
                openQty = openQty.subtract(opQty);
                if (openQty.signum() < 0) {
                    openQty = BigDecimal.ZERO;
                }
                if (openCost.signum() < 0) {
                    openCost = BigDecimal.ZERO;
                }
            }
        }

        batch.setActualProfit(realized);
    }

    /**
     * 根据累计买入/卖出数量重算批次阶段状态。
     *
     * <p>新规则：batch.quantity 仅作参考，不再参与判定。
     * <ul>
     *   <li>未买入：PENDING_BUY</li>
     *   <li>有买无卖：PARTIAL_BOUGHT</li>
     *   <li>有买有卖且卖 ≥ 买：COMPLETED</li>
     *   <li>有买有卖但卖 &lt; 买：PENDING_SELL</li>
     * </ul>
     */
    private StageStatus computeStageStatus(BigDecimal totalBuyQty,
                                           BigDecimal totalSellQty) {
        if (totalBuyQty.compareTo(BigDecimal.ZERO) <= 0) {
            return StageStatus.PENDING_BUY;
        }
        if (totalSellQty.compareTo(BigDecimal.ZERO) <= 0) {
            return StageStatus.PARTIAL_BOUGHT;
        }
        if (totalSellQty.compareTo(totalBuyQty) >= 0) {
            return StageStatus.COMPLETED;
        }
        return StageStatus.PENDING_SELL;
    }

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

    /** 兜底乐观锁冲突；正常路径不会触发（buy/sell 不要求 version）。 */
    private RealizationBatch saveBatchSafely(RealizationBatch batch) {
        try {
            return realizationBatchRepository.save(batch);
        } catch (OptimisticLockingFailureException ex) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("batchId=" + batch.getBatchId());
            return null;
        }
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 留作扩展：将来若按方向计算盈亏（短仓符号反向）可在此切入。 */
    @SuppressWarnings("unused")
    private BigDecimal applyDirectionSign(BatchDirection direction, BigDecimal value) {
        return value;
    }
}
