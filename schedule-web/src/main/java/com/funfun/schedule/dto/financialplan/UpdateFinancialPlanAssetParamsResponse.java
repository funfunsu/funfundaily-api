package com.funfun.schedule.dto.financialplan;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调整计划标的参数响应（API-5）。
 */
@Data
public class UpdateFinancialPlanAssetParamsResponse {

    /** 标的主键。 */
    private Long assetId;

    /** 计划买入单价。 */
    private BigDecimal planBuyPrice;

    /** 计划卖出单价。 */
    private BigDecimal planSellPrice;

    /** 计划买入数量。 */
    private BigDecimal planQuantity;

    /** 目标盈利 = (planSellPrice − planBuyPrice) × planQuantity。 */
    private BigDecimal targetProfit;
}
