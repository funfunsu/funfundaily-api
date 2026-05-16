package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.StageStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 登记兑现买入响应（API-7）。
 */
@Data
public class RecordRealizationBuyResponse {

    /** 批次主键。 */
    private Long batchId;

    /** 阶段状态。 */
    private StageStatus stageStatus;

    /** 累计实际买入金额。 */
    private BigDecimal actualBuyAmount;
}
