package com.funfun.schedule.dto.financialplan;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调整计划标的参数请求（API-5）。
 */
@Data
public class UpdateFinancialPlanAssetParamsRequest {

    /** 计划买入单价。 */
    private BigDecimal planBuyPrice;

    /** 计划卖出单价。 */
    private BigDecimal planSellPrice;

    /** 计划买入数量。 */
    private BigDecimal planQuantity;

    /** 乐观锁版本号（必填）。 */
    private Integer version;
}
