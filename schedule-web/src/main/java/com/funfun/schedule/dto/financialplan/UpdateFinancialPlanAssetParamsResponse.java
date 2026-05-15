package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.AssetMarket;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调整计划标的参数响应（API-5）。
 */
@Data
public class UpdateFinancialPlanAssetParamsResponse {

    /** 标的主键。 */
    private Long assetId;

    /** 股票名称。 */
    private String stockName;

    /** 所属市场。 */
    private AssetMarket market;

    /** 用户设定的目标盈利。 */
    private BigDecimal targetProfit;

    /** 乐观锁版本号。 */
    private Integer version;
}
