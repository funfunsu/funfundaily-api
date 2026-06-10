package com.funfun.schedule.dto;

import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登记兑现买入命令对象（正股或期权）。
 *
 * <p>新模型：可多次记录买入。后端将每次记录追加到 RealizationOperation。
 * instrument=OPTION 时需额外携带 optionType / strikePrice / expirationDate，价格可为 0。
 */
@Data
public class RecordRealizationBuyCommand {

    /** STOCK（默认）/ OPTION。 */
    private InstrumentType instrument;

    /** 实际成交日期。 */
    private LocalDate tradeDate;

    /** 实际买入单价（OPTION 可为 0）。 */
    private BigDecimal actualBuyPrice;

    /** 本次买入数量。 */
    private BigDecimal quantity;

    /** 本次费用，可为 0 / null。 */
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
