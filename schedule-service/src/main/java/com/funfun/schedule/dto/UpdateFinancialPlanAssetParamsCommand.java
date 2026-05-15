package com.funfun.schedule.dto;

import com.funfun.schedule.enums.AssetMarket;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调整计划标的参数命令对象（仅修改业务字段，主键不可变更）。
 */
@Data
public class UpdateFinancialPlanAssetParamsCommand {

    /** 股票名称。 */
    private String stockName;

    /** 所属市场。 */
    private AssetMarket market;

    /** 目标利润。 */
    private BigDecimal targetProfit;

    /** 乐观锁版本号。 */
    private Integer version;
}
