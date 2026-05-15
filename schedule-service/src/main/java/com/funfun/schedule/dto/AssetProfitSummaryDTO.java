package com.funfun.schedule.dto;

import com.funfun.schedule.enums.AssetMarket;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 标的维度收益汇总 DTO（对应 design.md § 3.2 AssetProfitSummary）。
 *
 * <p>金额单位为「该标的所属市场的原币种」；计划层做 CNY 汇总时按 {@link #market} 查汇率。
 */
@Data
public class AssetProfitSummaryDTO {

    /** 标的主键 */
    private Long assetId;

    /** 标的代码 */
    private String assetCode;

    /** 标的名称 */
    private String assetName;

    /** 所属市场（CN / US / HK），用于计划层换算 CNY。 */
    private AssetMarket market;

    /** 目标盈利：由用户在标的上设定。 */
    private BigDecimal targetProfit;

    /** 已计划盈利 = Σ 批次目标收益 = Σ (planSellPrice − planBuyPrice) × quantity。 */
    private BigDecimal plannedProfit;

    /** 已实现盈利 = Σ stageStatus=COMPLETED 批次的 actualProfit。 */
    private BigDecimal actualProfit;

    /** 该标的所有批次登记数量之和。 */
    private BigDecimal realizedQuantity;

    /** 该标的所有批次登记数量之和（与 realizedQuantity 等价；保留以兼容老消费者）。 */
    private BigDecimal plannedQuantity;

    /** 计划完成度 = plannedProfit / targetProfit，targetProfit=0 时置 0。 */
    private BigDecimal plannedCompletionRate;

    /** 实际完成度 = actualProfit / targetProfit，targetProfit=0 时置 0。 */
    private BigDecimal completionRate;
}
