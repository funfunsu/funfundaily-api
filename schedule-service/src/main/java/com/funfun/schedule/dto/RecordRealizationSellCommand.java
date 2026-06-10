package com.funfun.schedule.dto;

import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现卖出命令对象（正股或期权，可多次记录）。
 */
@Data
public class RecordRealizationSellCommand {

    /** STOCK（默认）/ OPTION。 */
    private InstrumentType instrument;

    /** 实际成交日期。 */
    private LocalDate tradeDate;

    /** 实际卖出单价（OPTION 可为 0）。 */
    private BigDecimal actualSellPrice;

    /** 本次卖出数量。 */
    private BigDecimal quantity;

    /** 本次费用。 */
    private BigDecimal fee;

    /** 期权类型：CALL / PUT；仅 OPTION 必填。 */
    private OptionType optionType;

    /** 目标价格（行权价）；仅 OPTION 必填。 */
    private BigDecimal strikePrice;

    /** 到期时间；仅 OPTION 必填。 */
    private LocalDate expirationDate;

    /** 备注。 */
    private String note;
}
