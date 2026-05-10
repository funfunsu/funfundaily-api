package com.funfun.schedule.dto.financialplan;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现买入请求（API-7）。
 */
@Data
public class RecordRealizationBuyRequest {

    /** 实际成交日期（必填）。 */
    private LocalDate tradeDate;

    /** 实际买入单价（必填）。 */
    private BigDecimal actualBuyPrice;

    /** 本次买入数量（必填，>0）。 */
    private BigDecimal quantity;

    /** 本次费用。 */
    private BigDecimal fee;

    /** 备注。 */
    private String note;

    /** 乐观锁版本号（必填）。 */
    private Integer version;
}
