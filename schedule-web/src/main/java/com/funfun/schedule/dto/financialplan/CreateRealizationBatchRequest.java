package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.BatchDirection;
import com.funfun.schedule.enums.BatchType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建兑现批次请求（API-6）。
 */
@Data
public class CreateRealizationBatchRequest {

    /** 标的主键（必填）。 */
    private Long assetId;

    /** 批次类型：EQUITY（正股）/ DERIVATIVE（衍生品）。 */
    private BatchType batchType;

    /** 衍生品方向：CALL / PUT / SHORT_CALL / SHORT_PUT；仅 batchType=DERIVATIVE 时必填。 */
    private BatchDirection direction;

    /** 批次名称；未填时由后端按 「{股票名}-{方向/类型}」生成。 */
    private String batchName;

    /** 数量，必填且为正。 */
    private BigDecimal quantity;

    /** 计划买入价，必填且为正。 */
    private BigDecimal planBuyPrice;

    /** 计划卖出价，必填且为正。 */
    private BigDecimal planSellPrice;

    /** 到期时间；仅 batchType=DERIVATIVE 时必填。 */
    private LocalDate expirationDate;

    /** 备注。 */
    private String note;
}
