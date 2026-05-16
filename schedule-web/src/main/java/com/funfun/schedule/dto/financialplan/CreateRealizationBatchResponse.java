package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.StageStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建兑现批次响应（API-6）。
 */
@Data
public class CreateRealizationBatchResponse {

    /** 批次主键。 */
    private Long batchId;

    /** 计划主键。 */
    private Long planId;

    /** 标的主键。 */
    private Long assetId;

    /** 批次计划兑现数量。 */
    private BigDecimal quantity;

    /** 批次阶段状态。 */
    private StageStatus stageStatus;
}
