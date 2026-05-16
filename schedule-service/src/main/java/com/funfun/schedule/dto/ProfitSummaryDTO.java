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

    /** 目标盈利 = Σ asset.targetProfit（由用户在各标的上设定）。 */
    private BigDecimal targetProfit;

    /** 已计划盈利 = Σ 各标的的 plannedProfit。 */
    private BigDecimal plannedProfit;

    /** 已实现盈利 = Σ COMPLETED 批次的 actualProfit。 */
    private BigDecimal actualProfit;

    /** 所有批次登记数量之和。 */
    private BigDecimal realizedQuantity;

    /** 所有批次登记数量之和（兼容字段）。 */
    private BigDecimal plannedQuantity;

    /** 计划完成度 = plannedProfit / targetProfit。 */
    private BigDecimal plannedCompletionRate;

    /** 实际完成度 = actualProfit / targetProfit。 */
    private BigDecimal completionRate;

    /** 已完成批次数量 */
    private int completedBatchCount;

    /** 未完成批次数量（批次存在但 stageStatus≠COMPLETED） */
    private int incompleteBatchCount;
}
