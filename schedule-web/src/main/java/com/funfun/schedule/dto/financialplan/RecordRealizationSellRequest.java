package com.funfun.schedule.dto.financialplan;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现卖出请求（API-8）。
 *
 * <p>新模型：同一批次可多次记录卖出；不再需要 version。
 */
@Data
public class RecordRealizationSellRequest {

    /** 实际成交日期（必填，前端默认当前日期，用户可改）。 */
    private LocalDate tradeDate;

    /** 实际卖出单价（必填）。 */
    private BigDecimal actualSellPrice;

    /** 本次卖出数量（必填，>0）。 */
    private BigDecimal quantity;

    /** 本次费用（可选，默认 0）。 */
    private BigDecimal fee;

    /** 备注。 */
    private String note;
}
