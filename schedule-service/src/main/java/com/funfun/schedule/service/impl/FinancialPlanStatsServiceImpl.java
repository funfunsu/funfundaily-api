package com.funfun.schedule.service.impl;

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

        BigDecimal targetProfit = ZERO;
        BigDecimal actualProfit = ZERO;
        BigDecimal realizedQuantity = ZERO;
        BigDecimal plannedQuantity = ZERO;
        int completedBatchCount = 0;
        int incompleteBatchCount = 0;

        // 计划层指标等于全部标的指标聚合（INV-6）
        for (AssetProfitSummaryDTO assetSummary : assetSummaries) {
            targetProfit = targetProfit.add(assetSummary.getTargetProfit());
            actualProfit = actualProfit.add(assetSummary.getActualProfit());
            realizedQuantity = realizedQuantity.add(assetSummary.getRealizedQuantity());
            plannedQuantity = plannedQuantity.add(assetSummary.getPlannedQuantity());
        }

        // 批次计数聚合（直接复用已拉取的批次数据）
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
        dto.setActualProfit(actualProfit);
        dto.setRealizedQuantity(realizedQuantity);
        dto.setPlannedQuantity(plannedQuantity);
        dto.setCompletionRate(calcRate(actualProfit, targetProfit));
        dto.setCompletedBatchCount(completedBatchCount);
        dto.setIncompleteBatchCount(incompleteBatchCount);
        return dto;
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
     * 计算单个标的收益汇总。
     *
     * <p>targetProfit = (planSellPrice − planBuyPrice) × planQuantity（INV-5，与兑现状态无关）。
     * actualProfit = Σ actualProfit where stageStatus=COMPLETED（INV-5）。
     * realizedQuantity = Σ quantity（全部批次登记数量，INV-6）。
     */
    private AssetProfitSummaryDTO buildAssetSummary(FinancialPlanAsset asset, List<RealizationBatch> batches) {
        // targetProfit 仅基于计划价格与计划数量，与兑现状态无关（INV-5）
        BigDecimal targetProfit = asset.getPlanSellPrice()
                .subtract(asset.getPlanBuyPrice())
                .multiply(asset.getPlanQuantity())
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal actualProfit = ZERO;
        BigDecimal realizedQuantity = ZERO;

        for (RealizationBatch batch : batches) {
            // 实际盈利仅聚合 COMPLETED 批次（INV-5）
            if (StageStatus.COMPLETED == batch.getStageStatus() && batch.getActualProfit() != null) {
                actualProfit = actualProfit.add(batch.getActualProfit());
            }
            // 已兑现数量聚合全部批次（INV-6）
            realizedQuantity = realizedQuantity.add(batch.getQuantity());
        }

        AssetProfitSummaryDTO dto = new AssetProfitSummaryDTO();
        dto.setAssetId(asset.getAssetId());
        dto.setAssetCode(asset.getAssetCode());
        dto.setAssetName(asset.getAssetName());
        dto.setTargetProfit(targetProfit);
        dto.setActualProfit(actualProfit);
        dto.setRealizedQuantity(realizedQuantity);
        dto.setPlannedQuantity(asset.getPlanQuantity());
        dto.setCompletionRate(calcRate(actualProfit, targetProfit));
        return dto;
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
