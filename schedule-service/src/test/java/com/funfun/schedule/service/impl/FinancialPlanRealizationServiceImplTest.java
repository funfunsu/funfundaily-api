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
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import com.funfun.schedule.repository.RealizationOperationRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link FinancialPlanRealizationServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class FinancialPlanRealizationServiceImplTest {

    @Mock
    private FinancialPlanRepository financialPlanRepository;

    @Mock
    private FinancialPlanAssetRepository financialPlanAssetRepository;

    @Mock
    private RealizationBatchRepository realizationBatchRepository;

    @Mock
    private RealizationOperationRepository realizationOperationRepository;

    @InjectMocks
    private FinancialPlanRealizationServiceImpl realizationService;

    /**
     * 验证 INV-3 反例：新建批次数量超过标的计划数量时应拒绝。
     */
    @Test
    void createBatchShouldRejectWhenQuantityExceedsPlanQuantity() {
        FinancialPlan plan = buildActivePlan(1L);
        FinancialPlanAsset asset = buildAsset(10L, 1L, new BigDecimal("100"), BigDecimal.ZERO);

        CreateRealizationBatchCommand command = new CreateRealizationBatchCommand();
        command.setAssetId(10L);
        command.setBatchName("batch-1");
        command.setQuantity(new BigDecimal("50"));

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(1L)).thenReturn(Optional.of(plan));
        when(financialPlanAssetRepository.findByAssetIdAndDeletedFalse(10L)).thenReturn(Optional.of(asset));
        when(realizationBatchRepository.sumBatchQuantityByAssetId(10L)).thenReturn(new BigDecimal("60"));

        MyException exception = assertThrows(MyException.class,
                () -> realizationService.createBatch(1L, command));

        assertEquals("FP_REALIZATION_QTY_EXCEEDED", exception.getCode());
    }

    /**
     * 验证 INV-4 反例：未有买入记录时，卖出登记必须失败。
     */
    @Test
    void recordSellShouldFailWhenNoBuyExists() {
        FinancialPlan plan = buildActivePlan(2L);
        RealizationBatch batch = buildBatch(20L, 2L, 100L, new BigDecimal("10"), StageStatus.PENDING_BUY, 1);

        RecordRealizationSellCommand command = new RecordRealizationSellCommand();
        command.setTradeDate(LocalDate.of(2026, 1, 2));
        command.setActualSellPrice(new BigDecimal("10"));
        command.setQuantity(new BigDecimal("1"));
        command.setFee(BigDecimal.ZERO);
        command.setVersion(1);

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(2L)).thenReturn(Optional.of(plan));
        when(realizationBatchRepository.findByBatchIdAndDeletedFalse(20L)).thenReturn(Optional.of(batch));
        when(realizationOperationRepository.findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(20L, OperationType.BUY))
                .thenReturn(List.of());

        MyException exception = assertThrows(MyException.class,
                () -> realizationService.recordSell(2L, 20L, command));

        assertEquals("FP_SELL_BEFORE_BUY", exception.getCode());
    }

    /**
     * 验证买卖顺序正例：先买后卖可进入 COMPLETED，并计算实际盈利与已兑现数量。
     */
    @Test
    void recordBuyThenSellShouldCompleteBatchAndSyncProfit() {
        FinancialPlan plan = buildActivePlan(3L);
        RealizationBatch batch = buildBatch(30L, 3L, 300L, new BigDecimal("10"), StageStatus.PENDING_BUY, 1);

        RecordRealizationBuyCommand buyCommand = new RecordRealizationBuyCommand();
        buyCommand.setTradeDate(LocalDate.of(2026, 2, 1));
        buyCommand.setActualBuyPrice(new BigDecimal("5"));
        buyCommand.setQuantity(new BigDecimal("10"));
        buyCommand.setFee(new BigDecimal("1"));
        buyCommand.setVersion(1);

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(3L)).thenReturn(Optional.of(plan));
        when(realizationBatchRepository.findByBatchIdAndDeletedFalse(30L)).thenReturn(Optional.of(batch));
        when(realizationOperationRepository.findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(30L, OperationType.BUY))
                .thenReturn(List.of());
        when(realizationOperationRepository.findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(30L, OperationType.SELL))
                .thenReturn(List.of());
        when(realizationOperationRepository.save(any(RealizationOperation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(realizationBatchRepository.save(any(RealizationBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RealizationBatch bought = realizationService.recordBuy(3L, 30L, buyCommand);

        assertEquals(StageStatus.PENDING_SELL, bought.getStageStatus());
        assertEquals(new BigDecimal("50"), bought.getActualBuyAmount());
        assertEquals(new BigDecimal("1"), bought.getFeeTotal());

        RecordRealizationSellCommand sellCommand = new RecordRealizationSellCommand();
        sellCommand.setTradeDate(LocalDate.of(2026, 2, 2));
        sellCommand.setActualSellPrice(new BigDecimal("8"));
        sellCommand.setQuantity(new BigDecimal("10"));
        sellCommand.setFee(new BigDecimal("2"));
        sellCommand.setVersion(1);

        FinancialPlanAsset asset = buildAsset(300L, 3L, new BigDecimal("100"), BigDecimal.ZERO);

        when(realizationOperationRepository.findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(30L, OperationType.BUY))
                .thenReturn(List.of(buildOperation(OperationType.BUY, new BigDecimal("10"))));
        when(realizationOperationRepository.findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(30L, OperationType.SELL))
                .thenReturn(List.of());
        when(financialPlanAssetRepository.findByAssetIdAndDeletedFalse(300L)).thenReturn(Optional.of(asset));
        when(financialPlanAssetRepository.save(any(FinancialPlanAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RealizationBatch sold = realizationService.recordSell(3L, 30L, sellCommand);

        assertEquals(StageStatus.COMPLETED, sold.getStageStatus());
        assertEquals(new BigDecimal("80"), sold.getActualSellAmount());
        assertNotNull(sold.getActualProfit());
        assertEquals(0, sold.getActualProfit().compareTo(new BigDecimal("27")));
        assertEquals(0, asset.getRealizedQuantity().compareTo(new BigDecimal("10")));
    }

    /**
     * 构造可编辑计划。
     */
    private FinancialPlan buildActivePlan(Long planId) {
        FinancialPlan plan = new FinancialPlan();
        plan.setPlanId(planId);
        plan.setStatus(PlanStatus.ACTIVE);
        return plan;
    }

    /**
     * 构造标的对象。
     */
    private FinancialPlanAsset buildAsset(Long assetId, Long planId, BigDecimal planQty, BigDecimal realizedQty) {
        FinancialPlanAsset asset = new FinancialPlanAsset();
        asset.setAssetId(assetId);
        asset.setPlanId(planId);
        asset.setPlanQuantity(planQty);
        asset.setRealizedQuantity(realizedQty);
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
                                        int version) {
        RealizationBatch batch = new RealizationBatch();
        batch.setBatchId(batchId);
        batch.setPlanId(planId);
        batch.setAssetId(assetId);
        batch.setQuantity(quantity);
        batch.setStageStatus(stageStatus);
        batch.setVersion(version);
        batch.setFeeTotal(BigDecimal.ZERO);
        return batch;
    }

    /**
     * 构造兑现流水。
     */
    private RealizationOperation buildOperation(OperationType type, BigDecimal quantity) {
        RealizationOperation operation = new RealizationOperation();
        operation.setOperationType(type);
        operation.setQuantity(quantity);
        return operation;
    }
}
