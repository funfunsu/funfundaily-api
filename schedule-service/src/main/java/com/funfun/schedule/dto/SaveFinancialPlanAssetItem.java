package com.funfun.schedule.dto;

import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计划标的批量保存项。
 *
 * <p>对应 API-4 请求体 items 数组中的单个标的。
 */
@Data
public class SaveFinancialPlanAssetItem {

    /** 已存在标的的主键，新增时为空。 */
    private Long assetId;

    /** 标的类型：SAVINGS / STOCK。 */
    private PlanType assetType;

    /** 标的代码，组合 (planId, assetCode, assetType) 不可重复。 */
    private String assetCode;

    /** 标的名称。 */
    private String assetName;

    /** 股票子类型：EQUITY / OPTION，仅股票类型有意义。 */
    private StockSubType stockSubType;

    /** 计划买入单价。 */
    private BigDecimal planBuyPrice;

    /** 计划卖出单价。 */
    private BigDecimal planSellPrice;

    /** 计划买入数量。 */
    private BigDecimal planQuantity;

    /** 计价币种。 */
    private String currency;

    /** 排序序号。 */
    private Integer sequenceNo;
}
