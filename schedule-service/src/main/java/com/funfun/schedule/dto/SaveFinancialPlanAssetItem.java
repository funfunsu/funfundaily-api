package com.funfun.schedule.dto;

import com.funfun.schedule.enums.AssetMarket;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计划标的批量保存项。
 *
 * <p>新模型：「股票名 + 市场 + 用户目标盈利」三件套；
 * 「已计划盈利」与「已实现盈利」由统计层从各批次聚合，不由用户输入。
 */
@Data
public class SaveFinancialPlanAssetItem {

    /** 已存在标的的主键，新增时为空。 */
    private Long assetId;

    /** 股票名称。 */
    private String stockName;

    /** 所属市场。 */
    private AssetMarket market;

    /** 用户设定的目标盈利。 */
    private BigDecimal targetProfit;

    /** 排序序号。 */
    private Integer sequenceNo;
}
