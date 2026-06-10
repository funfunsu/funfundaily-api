package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 单个兑现批次（=一只正股）的卡片汇总数据。
 *
 * <p>由操作明细按需计算，供前端批次卡片直观展示，不全部落库。
 */
@Data
public class BatchStatsDTO {

    /** 批次主键。 */
    private Long batchId;

    /** 目标收益 = (计划卖出价 - 计划买入价) × 计划数量。 */
    private BigDecimal targetProfit = BigDecimal.ZERO;

    /** 汇总正股已实现收益。 */
    private BigDecimal stockRealizedProfit = BigDecimal.ZERO;

    /** 汇总期权已实现收益。 */
    private BigDecimal optionRealizedProfit = BigDecimal.ZERO;

    /** 累计已实现收益（正股 + 期权）。 */
    private BigDecimal totalRealizedProfit = BigDecimal.ZERO;

    /** 正股净持仓数量。 */
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    /** 正股成本价格（持仓均价）；无持仓时为 null。 */
    private BigDecimal stockCostPrice;

    /** 正股持仓成本金额。 */
    private BigDecimal stockCostAmount = BigDecimal.ZERO;

    /** 各个期权 key 的持仓与盈亏。 */
    private List<OptionKeyStatsDTO> optionKeys = new ArrayList<>();
}
