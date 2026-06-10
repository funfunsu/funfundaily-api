package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现买入请求（API-7，正股或期权）。
 *
 * <p>同一批次可多次记录买入。instrument=OPTION 时需带 optionType/strikePrice/expirationDate，价格可为 0。
 */
@Data
public class RecordRealizationBuyRequest {

    /** STOCK（默认）/ OPTION。 */
    private InstrumentType instrument;

    /** 实际成交日期（必填，前端默认当前日期，用户可改）。 */
    private LocalDate tradeDate;

    /** 实际买入单价（必填；OPTION 可为 0）。 */
    private BigDecimal actualBuyPrice;

    /** 本次买入数量（必填；STOCK >0，OPTION ≠0）。 */
    private BigDecimal quantity;

    /** 本次费用（可选，默认 0）。 */
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
