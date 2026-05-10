package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.SaveFinancialPlanAssetItem;
import com.funfun.schedule.dto.UpdateFinancialPlanAssetParamsCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link PlanAssetServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PlanAssetServiceImplTest {

    @Mock
    private FinancialPlanRepository financialPlanRepository;

    @Mock
    private FinancialPlanAssetRepository financialPlanAssetRepository;

    @InjectMocks
    private PlanAssetServiceImpl planAssetService;

    /**
     * 验证 INV-7：归档计划不得再编辑标的。
     */
    @Test
    void saveAssetsShouldRejectArchivedPlan() {
        FinancialPlan archivedPlan = new FinancialPlan();
        archivedPlan.setPlanId(1L);
        archivedPlan.setStatus(PlanStatus.ARCHIVED);

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(1L)).thenReturn(Optional.of(archivedPlan));

        MyException exception = assertThrows(MyException.class,
                () -> planAssetService.saveAssets(1L, List.of(buildSavingsItem("BANK-001"))));

        assertEquals("FP_PLAN_ALREADY_ARCHIVED", exception.getCode());
    }

    /**
     * 验证股票分支：股票计划下 OPTION 标的允许保存。
     */
    @Test
    void saveAssetsShouldAcceptStockOptionItem() {
        FinancialPlan plan = new FinancialPlan();
        plan.setPlanId(2L);
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setPlanType(PlanType.STOCK);
        plan.setStockSubType(StockSubType.OPTION);

        SaveFinancialPlanAssetItem item = new SaveFinancialPlanAssetItem();
        item.setAssetType(PlanType.STOCK);
        item.setStockSubType(StockSubType.OPTION);
        item.setAssetCode("OPT-510050");
        item.setAssetName("510050 购权");
        item.setPlanBuyPrice(new BigDecimal("1.20"));
        item.setPlanSellPrice(new BigDecimal("1.80"));
        item.setPlanQuantity(new BigDecimal("100"));
        item.setCurrency("CNY");
        item.setSequenceNo(1);

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(2L)).thenReturn(Optional.of(plan));
        when(financialPlanAssetRepository.findByPlanIdAndAssetCodeAndAssetTypeAndDeletedFalse(2L, "OPT-510050", PlanType.STOCK))
                .thenReturn(Optional.empty());
        when(financialPlanAssetRepository.save(any(FinancialPlanAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(financialPlanAssetRepository.findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(2L))
                .thenAnswer(invocation -> List.of(invocation.getArgument(0) != null ? toSavedAsset(2L, item) : null));

        List<FinancialPlanAsset> saved = planAssetService.saveAssets(2L, List.of(item));

        assertEquals(1, saved.size());
        assertEquals(PlanType.STOCK, saved.get(0).getAssetType());
        assertEquals(StockSubType.OPTION, saved.get(0).getStockSubType());
    }

    /**
     * 验证 INV-2 反例：调整后 planQuantity 小于 realizedQuantity 时应拒绝。
     */
    @Test
    void updateAssetParamsShouldRejectQuantityLessThanRealized() {
        FinancialPlan plan = buildActiveSavingsPlan(3L);
        FinancialPlanAsset asset = buildAsset(30L, 3L, PlanType.SAVINGS, null,
                new BigDecimal("100"), new BigDecimal("60"), 1);

        UpdateFinancialPlanAssetParamsCommand command = new UpdateFinancialPlanAssetParamsCommand();
        command.setPlanBuyPrice(new BigDecimal("1"));
        command.setPlanSellPrice(new BigDecimal("1.2"));
        command.setPlanQuantity(new BigDecimal("59"));
        command.setVersion(1);

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(3L)).thenReturn(Optional.of(plan));
        when(financialPlanAssetRepository.findByAssetIdAndDeletedFalse(30L)).thenReturn(Optional.of(asset));

        MyException exception = assertThrows(MyException.class,
                () -> planAssetService.updateAssetParams(3L, 30L, command));

        assertEquals("FP_ASSET_QTY_LT_REALIZED", exception.getCode());
    }

    /**
     * 验证 INV-2 边界：调整后 planQuantity 等于 realizedQuantity 允许保存。
     */
    @Test
    void updateAssetParamsShouldAllowQuantityEqualToRealized() {
        FinancialPlan plan = buildActiveSavingsPlan(4L);
        FinancialPlanAsset asset = buildAsset(40L, 4L, PlanType.SAVINGS, null,
                new BigDecimal("100"), new BigDecimal("60"), 2);

        UpdateFinancialPlanAssetParamsCommand command = new UpdateFinancialPlanAssetParamsCommand();
        command.setPlanBuyPrice(new BigDecimal("2"));
        command.setPlanSellPrice(new BigDecimal("2.2"));
        command.setPlanQuantity(new BigDecimal("60"));
        command.setVersion(2);

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(4L)).thenReturn(Optional.of(plan));
        when(financialPlanAssetRepository.findByAssetIdAndDeletedFalse(40L)).thenReturn(Optional.of(asset));
        when(financialPlanAssetRepository.save(any(FinancialPlanAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancialPlanAsset saved = planAssetService.updateAssetParams(4L, 40L, command);

        assertEquals(new BigDecimal("60"), saved.getPlanQuantity());
        assertEquals(new BigDecimal("2"), saved.getPlanBuyPrice());
        assertEquals(new BigDecimal("2.2"), saved.getPlanSellPrice());
    }

    /**
     * 验证 INV-2 反例：同一请求内重复 (assetCode, assetType) 应拒绝。
     */
    @Test
    void saveAssetsShouldRejectDuplicatedAssetInSingleRequest() {
        FinancialPlan plan = buildActiveSavingsPlan(5L);

        SaveFinancialPlanAssetItem item1 = buildSavingsItem("BANK-001");
        SaveFinancialPlanAssetItem item2 = buildSavingsItem("BANK-001");

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(5L)).thenReturn(Optional.of(plan));

        MyException exception = assertThrows(MyException.class,
                () -> planAssetService.saveAssets(5L, List.of(item1, item2)));

        assertEquals("FP_ASSET_DUPLICATED", exception.getCode());
    }

    /**
     * 构造储蓄标的保存项。
     */
    private SaveFinancialPlanAssetItem buildSavingsItem(String code) {
        SaveFinancialPlanAssetItem item = new SaveFinancialPlanAssetItem();
        item.setAssetType(PlanType.SAVINGS);
        item.setAssetCode(code);
        item.setAssetName("bank-deposit");
        item.setPlanBuyPrice(new BigDecimal("1"));
        item.setPlanSellPrice(new BigDecimal("1.1"));
        item.setPlanQuantity(new BigDecimal("100"));
        item.setCurrency("CNY");
        item.setSequenceNo(1);
        return item;
    }

    /**
     * 构造激活状态储蓄计划。
     */
    private FinancialPlan buildActiveSavingsPlan(Long planId) {
        FinancialPlan plan = new FinancialPlan();
        plan.setPlanId(planId);
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setPlanType(PlanType.SAVINGS);
        return plan;
    }

    /**
     * 构造可用于更新场景的标的。
     */
    private FinancialPlanAsset buildAsset(Long assetId,
                                          Long planId,
                                          PlanType type,
                                          StockSubType subType,
                                          BigDecimal planQuantity,
                                          BigDecimal realizedQuantity,
                                          int version) {
        FinancialPlanAsset asset = new FinancialPlanAsset();
        asset.setAssetId(assetId);
        asset.setPlanId(planId);
        asset.setAssetType(type);
        asset.setStockSubType(subType);
        asset.setAssetCode("A-" + assetId);
        asset.setAssetName("asset-" + assetId);
        asset.setPlanBuyPrice(new BigDecimal("1"));
        asset.setPlanSellPrice(new BigDecimal("1.1"));
        asset.setPlanQuantity(planQuantity);
        asset.setRealizedQuantity(realizedQuantity);
        asset.setCurrency("CNY");
        asset.setSequenceNo(1);
        asset.setVersion(version);
        return asset;
    }

    /**
     * 将保存项映射为简化返回对象。
     */
    private FinancialPlanAsset toSavedAsset(Long planId, SaveFinancialPlanAssetItem item) {
        FinancialPlanAsset asset = new FinancialPlanAsset();
        asset.setPlanId(planId);
        asset.setAssetType(item.getAssetType());
        asset.setStockSubType(item.getStockSubType());
        asset.setAssetCode(item.getAssetCode());
        asset.setAssetName(item.getAssetName());
        asset.setPlanBuyPrice(item.getPlanBuyPrice());
        asset.setPlanSellPrice(item.getPlanSellPrice());
        asset.setPlanQuantity(item.getPlanQuantity());
        asset.setRealizedQuantity(BigDecimal.ZERO);
        asset.setCurrency(item.getCurrency());
        asset.setSequenceNo(item.getSequenceNo());
        return asset;
    }
}
