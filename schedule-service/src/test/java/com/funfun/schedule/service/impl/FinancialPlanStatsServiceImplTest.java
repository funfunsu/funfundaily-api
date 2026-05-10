package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.AssetProfitSummaryDTO;
import com.funfun.schedule.dto.ProgressSnapshotDTO;
import com.funfun.schedule.dto.ProfitSummaryDTO;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.StageStatus;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * {@link FinancialPlanStatsServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class FinancialPlanStatsServiceImplTest {

    @Mock
    private FinancialPlanRepository planRepository;

    @Mock
    private FinancialPlanAssetRepository assetRepository;

    @Mock
    private RealizationBatchRepository batchRepository;

    @InjectMocks
    private FinancialPlanStatsServiceImpl statsService;

    /**
     * 验证 INV-5/INV-6：计划层统计应由标的汇总得到，且仅 COMPLETED 批次计入 actualProfit。
     */
    @Test
    void calcPlanSummaryShouldAggregateAssetsAndCompletedProfits() {
        Long planId = 100L;

        FinancialPlanAsset assetA = buildAsset(1001L, "A", "Asset-A", new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("100"));
        FinancialPlanAsset assetB = buildAsset(1002L, "B", "Asset-B", new BigDecimal("5"), new BigDecimal("6"), new BigDecimal("200"));

        RealizationBatch aCompleted = buildBatch(1L, planId, 1001L, new BigDecimal("40"), StageStatus.COMPLETED, new BigDecimal("70"));
        RealizationBatch aPending = buildBatch(2L, planId, 1001L, new BigDecimal("10"), StageStatus.PENDING_SELL, null);
        RealizationBatch bCompleted = buildBatch(3L, planId, 1002L, new BigDecimal("60"), StageStatus.COMPLETED, new BigDecimal("20"));

        when(assetRepository.findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId))
                .thenReturn(List.of(assetA, assetB));
        when(batchRepository.findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(planId))
                .thenReturn(List.of(aCompleted, aPending, bCompleted));

        ProfitSummaryDTO summary = statsService.calcPlanSummary(planId);

        assertEquals(0, summary.getTargetProfit().compareTo(new BigDecimal("400.00000000")));
        assertEquals(0, summary.getActualProfit().compareTo(new BigDecimal("90")));
        assertEquals(0, summary.getRealizedQuantity().compareTo(new BigDecimal("110")));
        assertEquals(0, summary.getPlannedQuantity().compareTo(new BigDecimal("300")));
        assertEquals(0, summary.getCompletionRate().compareTo(new BigDecimal("0.22500000")));
        assertEquals(2, summary.getCompletedBatchCount());
        assertEquals(1, summary.getIncompleteBatchCount());
    }

    /**
     * 验证 INV-5：标的实际盈利只聚合 COMPLETED 批次。
     */
    @Test
    void calcAssetSummariesShouldIgnoreIncompleteBatchProfit() {
        Long planId = 101L;

        FinancialPlanAsset asset = buildAsset(2001L, "OPT", "Option-Asset", new BigDecimal("1"), new BigDecimal("1.5"), new BigDecimal("100"));
        RealizationBatch completed = buildBatch(11L, planId, 2001L, new BigDecimal("40"), StageStatus.COMPLETED, new BigDecimal("18"));
        RealizationBatch partial = buildBatch(12L, planId, 2001L, new BigDecimal("30"), StageStatus.PENDING_SELL, new BigDecimal("99"));

        when(assetRepository.findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId))
                .thenReturn(List.of(asset));
        when(batchRepository.findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(planId))
                .thenReturn(List.of(completed, partial));

        List<AssetProfitSummaryDTO> summaries = statsService.calcAssetSummaries(planId);

        assertEquals(1, summaries.size());
        AssetProfitSummaryDTO dto = summaries.get(0);
        assertEquals(0, dto.getTargetProfit().compareTo(new BigDecimal("50.00000000")));
        assertEquals(0, dto.getActualProfit().compareTo(new BigDecimal("18")));
        assertEquals(0, dto.getRealizedQuantity().compareTo(new BigDecimal("70")));
        assertEquals(0, dto.getCompletionRate().compareTo(new BigDecimal("0.36000000")));
    }

    /**
     * 验证 ProgressSnapshot 数值与告警：超窗口、存在未完成批次、数量达到计划。
     */
    @Test
    void calcProgressSnapshotShouldBuildRatesAndWarnings() {
        Long planId = 102L;
        LocalDate today = LocalDate.now();

        FinancialPlan plan = new FinancialPlan();
        plan.setPlanId(planId);
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setStartDate(today.minusDays(20));
        plan.setEndDate(today.minusDays(5));

        FinancialPlanAsset asset = buildAsset(3001L, "EQ", "Equity", new BigDecimal("9"), new BigDecimal("10"), new BigDecimal("100"));
        RealizationBatch completed = buildBatch(21L, planId, 3001L, new BigDecimal("100"), StageStatus.COMPLETED, new BigDecimal("100"));
        RealizationBatch pending = buildBatch(22L, planId, 3001L, new BigDecimal("10"), StageStatus.PENDING_SELL, null);

        when(planRepository.findByPlanIdAndDeletedFalse(planId)).thenReturn(Optional.of(plan));
        when(assetRepository.findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId)).thenReturn(List.of(asset));
        when(batchRepository.findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(planId))
                .thenReturn(List.of(completed, pending));

        ProgressSnapshotDTO snapshot = statsService.calcProgressSnapshot(planId);

        assertEquals("COMPLETED", snapshot.getPlanStatus());
        assertTrue(snapshot.getTimeProgressRate().compareTo(BigDecimal.ONE) > 0);
        assertEquals(0, snapshot.getQuantityProgressRate().compareTo(new BigDecimal("1.10000000")));
        assertEquals(0, snapshot.getProfitProgressRate().compareTo(new BigDecimal("1.00000000")));
        assertTrue(snapshot.getWarningFlags().contains("OVER_WINDOW"));
        assertTrue(snapshot.getWarningFlags().contains("INCOMPLETE_BATCH"));
        assertTrue(snapshot.getWarningFlags().contains("QUANTITY_REACHED"));
    }

    /**
     * 验证异常路径：计划不存在时应统一包装为 FP_STAT_CALC_FAILED。
     */
    @Test
    void calcProgressSnapshotShouldWrapExceptionToStatCalcFailed() {
        when(planRepository.findByPlanIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        MyException exception = assertThrows(MyException.class, () -> statsService.calcProgressSnapshot(999L));

        assertEquals("FP_STAT_CALC_FAILED", exception.getCode());
    }

    /**
     * 构造标的对象。
     */
    private FinancialPlanAsset buildAsset(Long assetId,
                                          String code,
                                          String name,
                                          BigDecimal buy,
                                          BigDecimal sell,
                                          BigDecimal quantity) {
        FinancialPlanAsset asset = new FinancialPlanAsset();
        asset.setAssetId(assetId);
        asset.setAssetCode(code);
        asset.setAssetName(name);
        asset.setPlanBuyPrice(buy);
        asset.setPlanSellPrice(sell);
        asset.setPlanQuantity(quantity);
        return asset;
    }

    /**
     * 构造批次对象。
     */
    private RealizationBatch buildBatch(Long batchId,
                                        Long planId,
                                        Long assetId,
                                        BigDecimal quantity,
                                        StageStatus stageStatus,
                                        BigDecimal actualProfit) {
        RealizationBatch batch = new RealizationBatch();
        batch.setBatchId(batchId);
        batch.setPlanId(planId);
        batch.setAssetId(assetId);
        batch.setQuantity(quantity);
        batch.setStageStatus(stageStatus);
        batch.setActualProfit(actualProfit);
        return batch;
    }
}
