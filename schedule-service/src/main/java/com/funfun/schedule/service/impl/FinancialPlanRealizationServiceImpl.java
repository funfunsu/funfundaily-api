package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.CreateRealizationBatchCommand;
import com.funfun.schedule.dto.ExerciseOptionCommand;
import com.funfun.schedule.dto.RecordRealizationBuyCommand;
import com.funfun.schedule.dto.RecordRealizationSellCommand;
import com.funfun.schedule.dto.UpdateRealizationBatchCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.BatchType;
import com.funfun.schedule.enums.ExerciseAction;
import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OperationType;
import com.funfun.schedule.enums.OptionType;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.StageStatus;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import com.funfun.schedule.repository.RealizationOperationRepository;
import com.funfun.schedule.service.FinancialPlanRealizationService;
import com.funfun.schedule.service.support.BatchStatsCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 兑现批次领域服务实现（#771 新模型）。
 *
 * <p>核心变化：
 * <ul>
 *   <li>批次只挂正股（EQUITY）；期权下沉为批次内的操作（instrument=OPTION）。</li>
 *   <li>买入/卖出可记录正股或期权；期权额外携带 optionType/strikePrice/expirationDate，
 *       价格可为 0、数量可为负。</li>
 *   <li>每次写操作后，从全部操作明细重算批次聚合字段（WAC，正股+期权已实现），
 *       聚合是「派生视图」，金额统一保留 2 位小数。</li>
 *   <li>提供行权/被行权：期权按价 0 平仓 + 自动建一条正股记录。</li>
 * </ul>
 */
@Service
public class FinancialPlanRealizationServiceImpl implements FinancialPlanRealizationService {

    private static final int MONEY_SCALE = 2;
    private static final int DIV_SCALE = 8;

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

    /** 为指定标的创建一个兑现批次（恒为正股 EQUITY）。 */
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
        batch.setBatchType(BatchType.EQUITY);
        batch.setDirection(null);
        batch.setExpirationDate(null);
        batch.setBatchName(resolveBatchName(asset, command));
        batch.setQuantity(command.getQuantity());
        batch.setPlanBuyPrice(round(command.getPlanBuyPrice()));
        batch.setPlanSellPrice(round(command.getPlanSellPrice()));
        batch.setStageStatus(StageStatus.PENDING_BUY);
        batch.setFeeTotal(BigDecimal.ZERO);
        batch.setActualProfit(BigDecimal.ZERO);
        batch.setNote(command.getNote());

        return realizationBatchRepository.save(batch);
    }

    /** 登记一次买入（正股或期权）。 */
    @Override
    @Transactional
    public RealizationBatch recordBuy(Long planId, Long batchId, RecordRealizationBuyCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);

        InstrumentType instrument = command.getInstrument() == null
                ? InstrumentType.STOCK : command.getInstrument();
        validateOperation(instrument, command.getTradeDate(), command.getActualBuyPrice(),
                command.getQuantity(), command.getFee(),
                command.getOptionType(), command.getStrikePrice(), command.getExpirationDate());

        RealizationOperation op = buildOperation(batchId, instrument, OperationType.BUY,
                command.getTradeDate(), command.getActualBuyPrice(), command.getQuantity(), command.getFee(),
                command.getOptionType(), command.getStrikePrice(), command.getExpirationDate(), command.getNote());
        realizationOperationRepository.save(op);

        recomputeBatchAggregates(batch);
        return saveBatchSafely(batch);
    }

    /** 登记一次卖出（正股或期权）。 */
    @Override
    @Transactional
    public RealizationBatch recordSell(Long planId, Long batchId, RecordRealizationSellCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);

        InstrumentType instrument = command.getInstrument() == null
                ? InstrumentType.STOCK : command.getInstrument();
        validateOperation(instrument, command.getTradeDate(), command.getActualSellPrice(),
                command.getQuantity(), command.getFee(),
                command.getOptionType(), command.getStrikePrice(), command.getExpirationDate());

        RealizationOperation op = buildOperation(batchId, instrument, OperationType.SELL,
                command.getTradeDate(), command.getActualSellPrice(), command.getQuantity(), command.getFee(),
                command.getOptionType(), command.getStrikePrice(), command.getExpirationDate(), command.getNote());
        realizationOperationRepository.save(op);

        recomputeBatchAggregates(batch);
        return saveBatchSafely(batch);
    }

    /**
     * 行权 / 被行权：对批次内某个期权 key 执行。
     *
     * <p>EXERCISE（净持仓>0）：期权按卖价 0 平掉全部多头 + 自动正股记录（CALL→买入，PUT→卖出）。
     * ASSIGN（净持仓<0）：期权按买价 0 平掉全部空头 + 自动正股记录（CALL→卖出，PUT→买入）。
     * 自动正股记录：价格 = strikePrice，数量 = |净持仓|（1:1 映射），tradeDate = 到期日。
     */
    @Override
    @Transactional
    public RealizationBatch exerciseOption(Long planId, Long batchId, ExerciseOptionCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        loadActivePlan(planId);
        RealizationBatch batch = loadBatchForWrite(batchId);
        ensureBatchBelongsToPlan(batch, planId);

        if (command.getOptionType() == null || command.getStrikePrice() == null
                || command.getExpirationDate() == null || command.getAction() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(
                    "optionType/strikePrice/expirationDate/action are required");
        }
        if (command.getStrikePrice().compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("strikePrice must be positive");
        }

        BigDecimal netQty = netOptionQuantity(batchId, command.getOptionType(),
                command.getStrikePrice(), command.getExpirationDate());
        BigDecimal absQty = netQty.abs();
        if (absQty.signum() == 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("option key has no open position");
        }

        OptionType optionType = command.getOptionType();
        LocalDate tradeDate = command.getExpirationDate();
        OperationType stockOpType;
        OperationType optionCloseType;
        if (command.getAction() == ExerciseAction.EXERCISE) {
            if (netQty.signum() <= 0) {
                FinancialPlanError.FP_STAGE_CONFLICT.throwsError("exercise requires long position (netQty>0)");
            }
            // 平掉多头：卖出期权（价 0）
            optionCloseType = OperationType.SELL;
            // CALL→买入正股，PUT→卖出正股
            stockOpType = optionType == OptionType.CALL ? OperationType.BUY : OperationType.SELL;
        } else {
            if (netQty.signum() >= 0) {
                FinancialPlanError.FP_STAGE_CONFLICT.throwsError("assign requires short position (netQty<0)");
            }
            // 平掉空头：买入期权（价 0）
            optionCloseType = OperationType.BUY;
            // CALL→卖出正股，PUT→买入正股
            stockOpType = optionType == OptionType.CALL ? OperationType.SELL : OperationType.BUY;
        }

        RealizationOperation closeOp = buildOperation(batchId, InstrumentType.OPTION, optionCloseType,
                tradeDate, BigDecimal.ZERO, absQty, BigDecimal.ZERO,
                optionType, round(command.getStrikePrice()), command.getExpirationDate(),
                command.getAction() == ExerciseAction.EXERCISE ? "行权平仓" : "被行权平仓");
        realizationOperationRepository.save(closeOp);

        RealizationOperation stockOp = buildOperation(batchId, InstrumentType.STOCK, stockOpType,
                tradeDate, round(command.getStrikePrice()), absQty, BigDecimal.ZERO,
                null, null, null,
                (command.getAction() == ExerciseAction.EXERCISE ? "行权" : "被行权") + "自动正股");
        realizationOperationRepository.save(stockOp);

        recomputeBatchAggregates(batch);
        return saveBatchSafely(batch);
    }

    /** 编辑批次的计划字段（正股计划）；数量必须为正，价格不为 0。 */
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

        if (command.getPlanBuyPrice() != null) {
            validatePositive("planBuyPrice", command.getPlanBuyPrice());
            batch.setPlanBuyPrice(round(command.getPlanBuyPrice()));
        }
        if (command.getPlanSellPrice() != null) {
            validatePositive("planSellPrice", command.getPlanSellPrice());
            batch.setPlanSellPrice(round(command.getPlanSellPrice()));
        }
        if (command.getQuantity() != null) {
            if (command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError("quantity must be positive");
            }
            batch.setQuantity(command.getQuantity());
        }
        if (command.getBatchName() != null) {
            String trimmed = command.getBatchName().trim();
            batch.setBatchName(trimmed.isEmpty() ? null : trimmed);
        }
        if (command.getNote() != null) {
            batch.setNote(command.getNote());
        }

        return saveBatchSafely(batch);
    }

    // ===================== 重算聚合 =====================

    /**
     * 从批次全部操作重算聚合字段（正股口径的金额 + 累计已实现 + 阶段状态 + 手续费）。
     */
    private void recomputeBatchAggregates(RealizationBatch batch) {
        List<RealizationOperation> ops = realizationOperationRepository
                .findByBatchIdOrderByTradeDateAscCreatedAtAsc(batch.getBatchId());

        BigDecimal stockBuyQty = BigDecimal.ZERO;
        BigDecimal stockBuyAmount = BigDecimal.ZERO;
        BigDecimal stockSellQty = BigDecimal.ZERO;
        BigDecimal stockSellAmount = BigDecimal.ZERO;
        BigDecimal feeTotal = BigDecimal.ZERO;
        LocalDate lastBuyDate = null;
        LocalDate lastSellDate = null;

        for (RealizationOperation op : ops) {
            feeTotal = feeTotal.add(nz(op.getFee()));
            if (op.getInstrument() == InstrumentType.OPTION) {
                continue;
            }
            BigDecimal amount = nz(op.getPrice()).multiply(nz(op.getQuantity()));
            if (op.getOperationType() == OperationType.BUY) {
                stockBuyQty = stockBuyQty.add(nz(op.getQuantity()));
                stockBuyAmount = stockBuyAmount.add(amount);
                lastBuyDate = op.getTradeDate();
            } else {
                stockSellQty = stockSellQty.add(nz(op.getQuantity()));
                stockSellAmount = stockSellAmount.add(amount);
                lastSellDate = op.getTradeDate();
            }
        }

        batch.setActualBuyAmount(round(stockBuyAmount));
        batch.setActualSellAmount(round(stockSellAmount));
        batch.setActualBuyPrice(stockBuyQty.signum() == 0 ? null : roundDiv(stockBuyAmount, stockBuyQty));
        batch.setActualSellPrice(stockSellQty.signum() == 0 ? null : roundDiv(stockSellAmount, stockSellQty));
        batch.setBuyTradeDate(lastBuyDate);
        batch.setSellTradeDate(lastSellDate);
        batch.setFeeTotal(round(feeTotal));
        batch.setStageStatus(computeStageStatus(stockBuyQty, stockSellQty));
        batch.setActualProfit(BatchStatsCalculator.computeStats(batch, ops).getTotalRealizedProfit());
    }

    /** 计算批次内指定期权 key 的净持仓（BUY +，SELL -）。 */
    private BigDecimal netOptionQuantity(Long batchId, OptionType optionType,
                                         BigDecimal strikePrice, LocalDate expirationDate) {
        List<RealizationOperation> ops = realizationOperationRepository
                .findByBatchIdOrderByTradeDateAscCreatedAtAsc(batchId);
        BigDecimal normStrike = round(strikePrice);
        BigDecimal net = BigDecimal.ZERO;
        for (RealizationOperation op : ops) {
            if (op.getInstrument() != InstrumentType.OPTION) {
                continue;
            }
            if (op.getOptionType() != optionType) {
                continue;
            }
            if (op.getStrikePrice() == null || op.getStrikePrice().compareTo(normStrike) != 0) {
                continue;
            }
            if (!Objects.equals(op.getExpirationDate(), expirationDate)) {
                continue;
            }
            BigDecimal q = nz(op.getQuantity());
            net = op.getOperationType() == OperationType.SELL ? net.subtract(q) : net.add(q);
        }
        return net;
    }

    // ===================== 校验 =====================

    /**
     * 操作校验：
     * <ul>
     *   <li>STOCK：价格 &gt; 0，数量 &gt; 0。</li>
     *   <li>OPTION：价格 ≥ 0（可为 0），数量 ≠ 0（可为负），optionType/strikePrice(&gt;0)/expirationDate 必填。</li>
     * </ul>
     */
    private void validateOperation(InstrumentType instrument, LocalDate tradeDate, BigDecimal price,
                                   BigDecimal quantity, BigDecimal fee,
                                   OptionType optionType, BigDecimal strikePrice, LocalDate expirationDate) {
        if (tradeDate == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("tradeDate is required");
        }
        if (price == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("price is required");
        }
        if (fee != null && fee.compareTo(BigDecimal.ZERO) < 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("fee must be non-negative");
        }
        if (instrument == InstrumentType.OPTION) {
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("option price must be >= 0");
            }
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
                FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError("option quantity must be non-zero");
            }
            if (optionType == null) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("optionType is required for OPTION");
            }
            if (strikePrice == null || strikePrice.compareTo(BigDecimal.ZERO) <= 0) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("strikePrice must be positive for OPTION");
            }
            if (expirationDate == null) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("expirationDate is required for OPTION");
            }
        } else {
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                FinancialPlanError.FP_VALIDATION_FAILED.throwsError("stock price must be positive");
            }
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError("stock quantity must be positive");
            }
        }
    }

    private void validatePositive(String field, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError(field + " must be positive");
        }
    }

    private void validateBatchCommand(CreateRealizationBatchCommand command) {
        if (command.getQuantity() == null || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_REALIZATION_QTY_EXCEEDED.throwsError("quantity must be positive");
        }
        validatePositive("planBuyPrice", command.getPlanBuyPrice());
        validatePositive("planSellPrice", command.getPlanSellPrice());
    }

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

    private String resolveBatchName(FinancialPlanAsset asset, CreateRealizationBatchCommand command) {
        if (command.getBatchName() != null && !command.getBatchName().trim().isEmpty()) {
            return command.getBatchName().trim();
        }
        return (asset.getStockName() == null ? "批次" : asset.getStockName()) + "-正股";
    }

    private StageStatus computeStageStatus(BigDecimal totalBuyQty, BigDecimal totalSellQty) {
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

    private RealizationOperation buildOperation(Long batchId, InstrumentType instrument, OperationType opType,
                                                LocalDate tradeDate, BigDecimal price, BigDecimal quantity,
                                                BigDecimal fee, OptionType optionType, BigDecimal strikePrice,
                                                LocalDate expirationDate, String note) {
        RealizationOperation op = new RealizationOperation();
        op.setBatchId(batchId);
        op.setInstrument(instrument == null ? InstrumentType.STOCK : instrument);
        op.setOperationType(opType);
        op.setTradeDate(tradeDate);
        op.setPrice(round(price));
        op.setQuantity(quantity);
        op.setFee(round(nz(fee)));
        op.setOptionType(instrument == InstrumentType.OPTION ? optionType : null);
        op.setStrikePrice(instrument == InstrumentType.OPTION ? round(strikePrice) : null);
        op.setExpirationDate(instrument == InstrumentType.OPTION ? expirationDate : null);
        op.setNote(note);
        return op;
    }

    private RealizationBatch saveBatchSafely(RealizationBatch batch) {
        try {
            return realizationBatchRepository.save(batch);
        } catch (OptimisticLockingFailureException ex) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("batchId=" + batch.getBatchId());
            return null;
        }
    }

    private BigDecimal round(BigDecimal v) {
        return v == null ? null : v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal roundDiv(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, DIV_SCALE, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
