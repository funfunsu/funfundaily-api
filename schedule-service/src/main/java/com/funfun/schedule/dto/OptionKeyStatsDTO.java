package com.funfun.schedule.dto;

import com.funfun.schedule.enums.OptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批次内单个期权 key（optionType + strikePrice + expirationDate）的持仓与盈亏汇总。
 */
@Data
public class OptionKeyStatsDTO {

    /** CALL / PUT。 */
    private OptionType optionType;

    /** 目标价格（行权价）。 */
    private BigDecimal strikePrice;

    /** 到期时间。 */
    private LocalDate expirationDate;

    /** 净持仓数量（>0 多头，<0 空头）。 */
    private BigDecimal netQuantity;

    /** 持仓成本金额（多头为正，空头为收到权利金净额，为负）。 */
    private BigDecimal costAmount;

    /** 持仓均价（按 |净持仓| 摊销）；无持仓时为 null。 */
    private BigDecimal avgCost;

    /** 该 key 的累计已实现盈亏。 */
    private BigDecimal realizedProfit;
}
