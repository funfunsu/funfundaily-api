package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.StageStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 登记兑现卖出响应（API-8）。
 */
@Data
public class RecordRealizationSellResponse {

    /** 批次主键。 */
    private Long batchId;

    /** 阶段状态。 */
    private StageStatus stageStatus;

    /** 累计实际盈利（仅 COMPLETED 后有值）。 */
    private BigDecimal actualProfit;
}
