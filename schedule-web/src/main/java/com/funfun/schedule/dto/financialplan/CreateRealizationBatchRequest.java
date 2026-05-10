package com.funfun.schedule.dto.financialplan;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建兑现批次请求（API-6）。
 */
@Data
public class CreateRealizationBatchRequest {

    /** 标的主键（必填）。 */
    private Long assetId;

    /** 批次名称。 */
    private String batchName;

    /** 批次计划兑现数量（必填，大于 0）。 */
    private BigDecimal quantity;

    /** 备注。 */
    private String note;
}
