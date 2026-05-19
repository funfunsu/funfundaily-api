package com.funfun.schedule.dto;

import com.funfun.schedule.enums.BatchDirection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 编辑兑现批次的命令对象。
 *
 * <p>不允许变更 batchType / planId / assetId；其余业务字段均可编辑。
 * 数量调整时后端会校验 quantity 不得小于已登记的累计买入/卖出数量。
 */
@Data
public class UpdateRealizationBatchCommand {

    /** 批次名称。 */
    private String batchName;

    /** 衍生品方向（DERIVATIVE 才生效）。 */
    private BatchDirection direction;

    /** 数量。 */
    private BigDecimal quantity;

    /** 预期买入价。 */
    private BigDecimal planBuyPrice;

    /** 预期卖出价。 */
    private BigDecimal planSellPrice;

    /** 到期日（DERIVATIVE 才生效）。 */
    private LocalDate expirationDate;

    /** 备注。 */
    private String note;

    /** 乐观锁版本号。 */
    private Integer version;
}
