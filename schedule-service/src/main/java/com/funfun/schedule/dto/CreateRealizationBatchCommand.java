package com.funfun.schedule.dto;

import com.funfun.schedule.enums.BatchDirection;
import com.funfun.schedule.enums.BatchType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建兑现批次命令对象。
 *
 * <p>新模型：EQUITY 仅需 quantity + planBuyPrice + planSellPrice；
 * DERIVATIVE 额外携带 direction 和 expirationDate。
 */
@Data
public class CreateRealizationBatchCommand {

    /** 标的主键。 */
    private Long assetId;

    /** 批次类型：EQUITY / DERIVATIVE。 */
    private BatchType batchType;

    /** 衍生品方向；仅 batchType=DERIVATIVE 时必填。 */
    private BatchDirection direction;

    /** 批次名称（可选，前端如未填则后端按规则生成）。 */
    private String batchName;

    /** 数量，必须为正。 */
    private BigDecimal quantity;

    /** 计划买入价，必须为正。 */
    private BigDecimal planBuyPrice;

    /** 计划卖出价，必须为正。 */
    private BigDecimal planSellPrice;

    /** 到期日；仅 batchType=DERIVATIVE 时必填。 */
    private LocalDate expirationDate;

    /** 备注。 */
    private String note;
}
