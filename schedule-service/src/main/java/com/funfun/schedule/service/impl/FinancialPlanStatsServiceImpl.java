package com.funfun.schedule.service.impl;

import com.funfun.schedule.config.ExchangeRateConfig;
import com.funfun.schedule.dto.AssetProfitSummaryDTO;
import com.funfun.schedule.dto.ProfitSummaryDTO;
import com.funfun.schedule.dto.ProgressSnapshotDTO;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.StageStatus;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import com.funfun.schedule.service.FinancialPlanStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link FinancialPlanStatsService} 实现。
 *
 * <p>所有计算在一个只读事务内完成，避免脏读；计算过程中任何运行时异常均被捕获、
 * 记录可观测日志后以 FP_STAT_CALC_FAILED 抛出（INV-5/INV-6）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialPlanStatsServiceImpl implements FinancialPlanStatsService {

    private static final int SCALE = 8;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FinancialPlanRepository planRepository;
    private final FinancialPlanAssetRepository assetRepository;
    private final RealizationBatchRepository batchRepository;
    private final ExchangeRateConfig exchangeRateConfig;

    /**
     * 计算计划维度收益汇总（INV-6：计划层 = 全部标的汇总）。
     *
     * @param planId 计划主键
     * @return 计划维度收益汇总
     */
    @Override
    @Transactional(readOnly = true)
    public ProfitSummaryDTO calcPlanSummary(Long planId) {
        try {
            return buildPlanSummaryInternal(planId);
        } catch (Exception e) {
            log.error("[FinancialPlanStats] calcPlanSummary failed, planId={}", planId, e);
            FinancialPlanError.FP_STAT_CALC_FAILED.throwsError(planId);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 计算各标的维度收益汇总列表（顺序与标的 sequenceNo 一致）。
     *
     * @param planId 计划主键
     * @return 标的维度收益汇总列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<AssetProfitSummaryDTO> calcAssetSummaries(Long planId) {
        try {
            return buildAssetSummaries(planId);
        } catch (Exception e) {
            log.error("[FinancialPlanStats] calcAssetSummaries failed, planId={}", planId, e);
            FinancialPlanError.FP_STAT_CALC_FAILED.throwsError(planId);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 计算计划执行进度快照（时间 / 数量 / 收益进度率及告警标志）。
     *
     * @param planId 计划主键
     * @return 执行进度快照
     */
    @Override
    @Transactional(readOnly = true)
    public ProgressSnapshotDTO calcProgressSnapshot(Long planId) {
        try {
            FinancialPlan plan = planRepository.findByPlanIdAndDeletedFalse(planId)
                    .orElseThrow(() -> new IllegalArgumentException("planId=" + planId));

            // 直接使用内部方法避免跨公共方法重复异常包装
            ProfitSummaryDTO planSummary = buildPlanSummaryInternal(planId);

            LocalDate today = LocalDate.now();
            LocalDate startDate = plan.getStartDate();
            LocalDate endDate = plan.getEndDate();

            BigDecimal timeProgressRate = calcTimeProgressRate(today, startDate, endDate);
            BigDecimal quantityProgressRate =
                    calcRate(planSummary.getRealizedQuantity(), planSummary.getPlannedQuantity());
            BigDecimal profitProgressRate =
                    calcRate(planSummary.getActualProfit(), planSummary.getTargetProfit());

            String planStatus = resolvePlanStatus(plan.getStatus(), today, startDate, endDate,
                    planSummary.getRealizedQuantity(), planSummary.getPlannedQuantity());

            List<String> warningFlags = new ArrayList<>();
            // OVER_WINDOW：已超出计划结束日期
            if (today.isAfter(endDate)) {
                warningFlags.add("OVER_WINDOW");
            }
            // INCOMPLETE_BATCH：存在未完成兑现批次
            if (planSummary.getIncompleteBatchCount() > 0) {
                warningFlags.add("INCOMPLETE_BATCH");
            }
            // QUANTITY_REACHED：已兑现数量已达到计划数量
            if (planSummary.getPlannedQuantity().compareTo(ZERO) > 0
                    && planSummary.getRealizedQuantity().compareTo(planSummary.getPlannedQuantity()) >= 0) {
                warningFlags.add("QUANTITY_REACHED");
            }

            ProgressSnapshotDTO dto = new ProgressSnapshotDTO();
            dto.setPlanStatus(planStatus);
            dto.setTimeProgressRate(timeProgressRate);
            dto.setQuantityProgressRate(quantityProgressRate);
            dto.setProfitProgressRate(profitProgressRate);
            dto.setWarningFlags(warningFlags);
            return dto;

        } catch (Exception e) {
            log.error("[FinancialPlanStats] calcProgressSnapshot failed, planId={}", planId, e);
            FinancialPlanError.FP_STAT_CALC_FAILED.throwsError(planId);
            throw new IllegalStateException("unreachable");
        }
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------------------------

    /**
     * 内部计划汇总构建，不做异常包装；供公共方法和 calcProgressSnapshot 共用。
     */
    private ProfitSummaryDTO buildPlanSummaryInternal(Long planId) {
        List<AssetProfitSummaryDTO> assetSummaries = buildAssetSummaries(planId);

        // 计划层目标盈利由计划本身承载（用户在计划上的设定），不再 Σ 各标的的 targetProfit。
        FinancialPlan plan = planRepository.findByPlanIdAndDeletedFalse(planId)
                .orElseThrow(() -> new IllegalArgumentException("planId=" + planId));
        BigDecimal targetProfit = nullSafe(plan.getTargetProfit());

        BigDecimal plannedProfit = ZERO;
        BigDecimal actualProfit = ZERO;
        BigDecimal realizedQuantity = ZERO;
        BigDecimal plannedQuantity = ZERO;
        int completedBatchCount = 0;
        int incompleteBatchCount = 0;

        // 已计划盈利 / 已实现盈利：把每个标的的原币种金额按其市场汇率换算成 CNY 后再求和。
        // 数量字段不涉及币种，直接累加即可。
        for (AssetProfitSummaryDTO assetSummary : assetSummaries) {
            BigDecimal rate = exchangeRateConfig.resolveRate(assetSummary.getMarket());
            plannedProfit = plannedProfit.add(nullSafe(assetSummary.getPlannedProfit()).multiply(rate));
            actualProfit = actualProfit.add(nullSafe(assetSummary.getActualProfit()).multiply(rate));
            realizedQuantity = realizedQuantity.add(nullSafe(assetSummary.getRealizedQuantity()));
            plannedQuantity = plannedQuantity.add(nullSafe(assetSummary.getPlannedQuantity()));
        }

        plannedProfit = plannedProfit.setScale(SCALE, RoundingMode.HALF_UP);
        actualProfit = actualProfit.setScale(SCALE, RoundingMode.HALF_UP);

        // 批次计数聚合
        List<RealizationBatch> allBatches =
                batchRepository.findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(planId);
        for (RealizationBatch batch : allBatches) {
            if (StageStatus.COMPLETED == batch.getStageStatus()) {
                completedBatchCount++;
            } else {
                incompleteBatchCount++;
            }
        }

        ProfitSummaryDTO dto = new ProfitSummaryDTO();
        dto.setPlanId(planId);
        dto.setTargetProfit(targetProfit);
        dto.setPlannedProfit(plannedProfit);
        dto.setActualProfit(actualProfit);
        dto.setRealizedQuantity(realizedQuantity);
        dto.setPlannedQuantity(plannedQuantity);
        dto.setPlannedCompletionRate(calcRate(plannedProfit, targetProfit));
        dto.setCompletionRate(calcRate(actualProfit, targetProfit));
        dto.setCompletedBatchCount(completedBatchCount);
        dto.setIncompleteBatchCount(incompleteBatchCount);
        return dto;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    /**
     * 构造全部标的维度收益汇总；供 calcPlanSummary / calcAssetSummaries 共用。
     */
    private List<AssetProfitSummaryDTO> buildAssetSummaries(Long planId) {
        // 按 sequenceNo 升序排列的标的列表
        List<FinancialPlanAsset> assets =
                assetRepository.findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId);

        // 预取该计划所有批次并按 assetId 分组，减少 N+1 查询
        List<RealizationBatch> allBatches =
                batchRepository.findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(planId);
        Map<Long, List<RealizationBatch>> batchesByAssetId = allBatches.stream()
                .collect(Collectors.groupingBy(RealizationBatch::getAssetId));

        List<AssetProfitSummaryDTO> result = new ArrayList<>();
        for (FinancialPlanAsset asset : assets) {
            result.add(buildAssetSummary(asset, batchesByAssetId.getOrDefault(asset.getAssetId(), List.of())));
        }
        return result;
    }

    /**
     * 计算单个标的收益汇总（新模型）。
     *
     * <p>标的层所有金额都保持「市场原币种」：用户在某只股票上设定的目标盈利 / 批次目标收益 / 实际盈利
     * 都属于同一市场，单位一致，做完成度比较没有歧义。
     * CNY 换算只在「计划层汇总」做（见 buildPlanSummaryInternal）。
     */
    private AssetProfitSummaryDTO buildAssetSummary(FinancialPlanAsset asset, List<RealizationBatch> batches) {
        BigDecimal targetProfit = asset.getTargetProfit() == null
                ? ZERO
                : asset.getTargetProfit().setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal plannedProfit = ZERO;
        BigDecimal actualProfit = ZERO;
        BigDecimal totalBatchQuantity = ZERO;

        for (RealizationBatch batch : batches) {
            plannedProfit = plannedProfit.add(computeBatchTargetProfit(batch));
            if (StageStatus.COMPLETED == batch.getStageStatus() && batch.getActualProfit() != null) {
                actualProfit = actualProfit.add(batch.getActualProfit());
            }
            if (batch.getQuantity() != null) {
                totalBatchQuantity = totalBatchQuantity.add(batch.getQuantity());
            }
        }

        plannedProfit = plannedProfit.setScale(SCALE, RoundingMode.HALF_UP);
        actualProfit = actualProfit.setScale(SCALE, RoundingMode.HALF_UP);

        AssetProfitSummaryDTO dto = new AssetProfitSummaryDTO();
        dto.setAssetId(asset.getAssetId());
        dto.setAssetCode(asset.getStockName());
        dto.setAssetName(asset.getStockName());
        dto.setMarket(asset.getMarket());
        dto.setTargetProfit(targetProfit);
        dto.setPlannedProfit(plannedProfit);
        dto.setActualProfit(actualProfit);
        dto.setRealizedQuantity(totalBatchQuantity);
        dto.setPlannedQuantity(totalBatchQuantity);
        dto.setPlannedCompletionRate(calcRate(plannedProfit, targetProfit));
        dto.setCompletionRate(calcRate(actualProfit, targetProfit));
        return dto;
    }

    /** 单个批次的目标收益：(planSellPrice − planBuyPrice) × quantity；任意字段缺失则为 0。 */
    private BigDecimal computeBatchTargetProfit(RealizationBatch batch) {
        BigDecimal buy = batch.getPlanBuyPrice();
        BigDecimal sell = batch.getPlanSellPrice();
        BigDecimal qty = batch.getQuantity();
        if (buy == null || sell == null || qty == null) {
            return ZERO;
        }
        return sell.subtract(buy).multiply(qty);
    }

    /**
     * 计算时间进度率 = (today − startDate) / (endDate − startDate)。
     *
     * <ul>
     *   <li>today &lt; startDate 时返回 0</li>
     *   <li>startDate == endDate 时返回 1</li>
     *   <li>结果可超过 1（表示已超期）</li>
     * </ul>
     */
    private BigDecimal calcTimeProgressRate(LocalDate today, LocalDate startDate, LocalDate endDate) {
        if (today.isBefore(startDate)) {
            return ZERO;
        }
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (totalDays <= 0) {
            return BigDecimal.ONE;
        }
        long elapsedDays = ChronoUnit.DAYS.between(startDate, today);
        return BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalDays), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算进度率 = numerator / denominator，分母为 0 时返回 0。
     */
    private BigDecimal calcRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 根据计划状态与时间/数量信息推断展示状态字符串。
     */
    private String resolvePlanStatus(PlanStatus status, LocalDate today,
                                     LocalDate startDate, LocalDate endDate,
                                     BigDecimal realizedQuantity, BigDecimal plannedQuantity) {
        if (PlanStatus.ARCHIVED == status) {
            return "COMPLETED";
        }
        if (today.isBefore(startDate)) {
            return "NOT_STARTED";
        }
        if (plannedQuantity.compareTo(ZERO) > 0
                && realizedQuantity.compareTo(plannedQuantity) >= 0) {
            return "COMPLETED";
        }
        if (realizedQuantity.compareTo(ZERO) > 0) {
            return "PARTIAL";
        }
        return "IN_PROGRESS";
    }
}
