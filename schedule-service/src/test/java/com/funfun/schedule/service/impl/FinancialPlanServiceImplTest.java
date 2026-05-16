package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.CreateFinancialPlanCommand;
import com.funfun.schedule.dto.UpdateFinancialPlanCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.enums.TimeRangeType;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link FinancialPlanServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class FinancialPlanServiceImplTest {

    @Mock
    private FinancialPlanRepository financialPlanRepository;

    @Mock
    private FinancialPlanAssetRepository financialPlanAssetRepository;

    @Mock
    private RealizationBatchRepository realizationBatchRepository;

    @InjectMocks
    private FinancialPlanServiceImpl financialPlanService;

    /**
     * 验证 INV-1：YEAR 时间窗口会由 fiscalYear 派生自然年起止日期。
     */
    @Test
    void createPlanWithYearWindowShouldDeriveNaturalYearDates() {
        CreateFinancialPlanCommand command = new CreateFinancialPlanCommand();
        command.setGroupId(1L);
        command.setOwnerUserId(2L);
        command.setPlanName("year-plan");
        command.setPlanType(PlanType.SAVINGS);
        command.setTimeRangeType(TimeRangeType.YEAR);
        command.setFiscalYear(2026);

        when(financialPlanRepository.save(any(FinancialPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancialPlan plan = financialPlanService.createPlan(command);

        assertEquals(TimeRangeType.YEAR, plan.getTimeRangeType());
        assertEquals(2026, plan.getFiscalYear());
        assertEquals(LocalDate.of(2026, 1, 1), plan.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), plan.getEndDate());
        assertEquals(PlanStatus.DRAFT, plan.getStatus());
        assertNull(plan.getStockSubType());
    }

    /**
     * 验证 INV-1：CUSTOM 时间窗口允许用户自定义起止日期且满足 startDate <= endDate。
     */
    @Test
    void createPlanWithCustomWindowShouldKeepInputDates() {
        CreateFinancialPlanCommand command = new CreateFinancialPlanCommand();
        command.setGroupId(1L);
        command.setOwnerUserId(2L);
        command.setPlanName("custom-plan");
        command.setPlanType(PlanType.SAVINGS);
        command.setTimeRangeType(TimeRangeType.CUSTOM);
        command.setStartDate(LocalDate.of(2026, 2, 1));
        command.setEndDate(LocalDate.of(2026, 3, 31));

        when(financialPlanRepository.save(any(FinancialPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancialPlan plan = financialPlanService.createPlan(command);

        assertEquals(TimeRangeType.CUSTOM, plan.getTimeRangeType());
        assertNull(plan.getFiscalYear());
        assertEquals(LocalDate.of(2026, 2, 1), plan.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 31), plan.getEndDate());
    }

    /**
     * 验证 INV-1 反例：CUSTOM 时间窗口中 startDate > endDate 时应拒绝。
     */
    @Test
    void createPlanWithInvalidCustomWindowShouldThrowWindowInvalid() {
        CreateFinancialPlanCommand command = new CreateFinancialPlanCommand();
        command.setGroupId(1L);
        command.setOwnerUserId(2L);
        command.setPlanName("invalid-window");
        command.setPlanType(PlanType.SAVINGS);
        command.setTimeRangeType(TimeRangeType.CUSTOM);
        command.setStartDate(LocalDate.of(2026, 5, 2));
        command.setEndDate(LocalDate.of(2026, 5, 1));

        MyException exception = assertThrows(MyException.class, () -> financialPlanService.createPlan(command));

        assertEquals("FP_WINDOW_INVALID", exception.getCode());
    }

    /**
     * 验证股票计划分支：STOCK 计划可接收 OPTION 子类型并正常创建。
     */
    @Test
    void createStockPlanWithOptionSubtypeShouldSucceed() {
        CreateFinancialPlanCommand command = new CreateFinancialPlanCommand();
        command.setGroupId(1L);
        command.setOwnerUserId(2L);
        command.setPlanName("stock-option-plan");
        command.setPlanType(PlanType.STOCK);
        command.setStockSubType(StockSubType.OPTION);
        command.setTimeRangeType(TimeRangeType.YEAR);
        command.setFiscalYear(2026);

        when(financialPlanRepository.save(any(FinancialPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancialPlan plan = financialPlanService.createPlan(command);

        assertEquals(PlanType.STOCK, plan.getPlanType());
        assertEquals(StockSubType.OPTION, plan.getStockSubType());
    }

    /**
     * 验证归档不变量 INV-7：已归档计划禁止编辑。
     */
    @Test
    void updateArchivedPlanShouldRejectModification() {
        FinancialPlan archivedPlan = new FinancialPlan();
        archivedPlan.setPlanId(10L);
        archivedPlan.setStatus(PlanStatus.ARCHIVED);
        archivedPlan.setVersion(7);

        UpdateFinancialPlanCommand command = new UpdateFinancialPlanCommand();
        command.setVersion(7);
        command.setStatus(PlanStatus.ACTIVE);
        command.setPlanName("should-fail");

        when(financialPlanRepository.findByPlanIdAndDeletedFalse(10L)).thenReturn(Optional.of(archivedPlan));

        MyException exception = assertThrows(MyException.class,
                () -> financialPlanService.updatePlan(10L, command));

        assertEquals("FP_PLAN_ALREADY_ARCHIVED", exception.getCode());
    }
}
