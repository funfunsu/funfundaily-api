package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建兑现批次命令对象。
 *
 * <p>对应 API-6 创建批次，由 schedule-web 控制器层映射后传入服务层。
 */
@Data
public class CreateRealizationBatchCommand {

    /** 标的主键。 */
    private Long assetId;

    /** 批次名称（用户可读）。 */
    private String batchName;

    /** 批次计划兑现数量，必须大于 0。 */
    private BigDecimal quantity;

    /** 备注。 */
    private String note;
}
