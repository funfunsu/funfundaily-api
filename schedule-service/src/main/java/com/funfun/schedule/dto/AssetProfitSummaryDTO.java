package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 标的维度收益汇总 DTO（对应 design.md § 3.2 AssetProfitSummary）。
 */
@Data
public class AssetProfitSummaryDTO {

    /** 标的主键 */
    private Long assetId;

    /** 标的代码 */
    private String assetCode;

    /** 标的名称 */
    private String assetName;

    /** 目标盈利 = (planSellPrice − planBuyPrice) × planQuantity（INV-5） */
    private BigDecimal targetProfit;

    /** 实际盈利 = 仅聚合 stageStatus=COMPLETED 批次的 actualProfit（INV-5） */
    private BigDecimal actualProfit;

    /** 该标的所有批次登记数量之和（INV-6） */
    private BigDecimal realizedQuantity;

    /** 该标的计划数量 */
    private BigDecimal plannedQuantity;

    /** 完成率 = actualProfit / targetProfit，targetProfit=0 时置 0 */
    private BigDecimal completionRate;
}
