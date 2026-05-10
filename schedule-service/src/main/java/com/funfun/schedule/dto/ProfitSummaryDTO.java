package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 计划维度收益汇总 DTO（对应 design.md § 3.2 ProfitSummary）。
 *
 * <p>targetProfit、actualProfit 均为精度保留的大数；
 * completionRate 为 [0, 1] 区间的小数，前端按需格式化为百分比展示。
 */
@Data
public class ProfitSummaryDTO {

    /** 计划主键 */
    private Long planId;

    /** 目标盈利 = Σ(planSellPrice − planBuyPrice) × planQuantity（INV-5） */
    private BigDecimal targetProfit;

    /** 实际盈利 = 仅聚合 stageStatus=COMPLETED 批次的 actualProfit（INV-5） */
    private BigDecimal actualProfit;

    /** 所有批次登记数量之和（INV-6） */
    private BigDecimal realizedQuantity;

    /** 计划总数量（全部标的 planQuantity 之和） */
    private BigDecimal plannedQuantity;

    /** 完成率 = actualProfit / targetProfit，targetProfit=0 时置 0 */
    private BigDecimal completionRate;

    /** 已完成批次数量 */
    private int completedBatchCount;

    /** 未完成批次数量（批次存在但 stageStatus≠COMPLETED） */
    private int incompleteBatchCount;
}
