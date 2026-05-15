package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现买入命令对象。
 *
 * <p>新模型：可多次记录买入。后端将每次记录追加到 RealizationOperation，
 * 不再对批次本身做版本号校验（不会引发 FP_VERSION_CONFLICT）。
 */
@Data
public class RecordRealizationBuyCommand {

    /** 实际成交日期。 */
    private LocalDate tradeDate;

    /** 实际买入单价。 */
    private BigDecimal actualBuyPrice;

    /** 本次买入数量，必须大于 0。 */
    private BigDecimal quantity;

    /** 本次费用，可为 0 / null。 */
    private BigDecimal fee;

    /** 备注。 */
    private String note;
}
