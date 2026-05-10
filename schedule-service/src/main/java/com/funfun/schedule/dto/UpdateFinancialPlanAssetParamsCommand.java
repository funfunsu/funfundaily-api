package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调整计划标的参数命令对象。
 *
 * <p>对应 API-5 调整标的参数，仅允许调整价格与数量；不变量 INV-2 由服务层校验。
 */
@Data
public class UpdateFinancialPlanAssetParamsCommand {

    /** 计划买入单价。 */
    private BigDecimal planBuyPrice;

    /** 计划卖出单价。 */
    private BigDecimal planSellPrice;

    /** 计划买入数量，调整后不得小于该标的已兑现数量。 */
    private BigDecimal planQuantity;

    /** 乐观锁版本号。 */
    private Integer version;
}
