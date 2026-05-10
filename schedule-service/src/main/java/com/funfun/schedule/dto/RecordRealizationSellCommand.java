package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现卖出命令对象。
 *
 * <p>对应 API-8，将一次实际卖出动作累加到批次上。
 */
@Data
public class RecordRealizationSellCommand {

    /** 实际成交日期。 */
    private LocalDate tradeDate;

    /** 实际卖出单价。 */
    private BigDecimal actualSellPrice;

    /** 本次卖出数量，必须大于 0。 */
    private BigDecimal quantity;

    /** 本次费用（手续费、税等），可为 0。 */
    private BigDecimal fee;

    /** 备注。 */
    private String note;

    /** 乐观锁版本号。 */
    private Integer version;
}
