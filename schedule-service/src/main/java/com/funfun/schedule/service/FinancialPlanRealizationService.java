package com.funfun.schedule.service;

import com.funfun.schedule.dto.CreateRealizationBatchCommand;
import com.funfun.schedule.dto.RecordRealizationBuyCommand;
import com.funfun.schedule.dto.RecordRealizationSellCommand;
import com.funfun.schedule.entity.RealizationBatch;

/**
 * 兑现批次领域服务。
 *
 * <p>承载批次创建、买入登记、卖出登记与状态流转；
 * 严格遵循 INV-3（数量上限）、INV-4（卖出需先有买入）、INV-6（聚合一致）、
 * INV-7（归档计划禁止写入）等不变量。
 */
public interface FinancialPlanRealizationService {

    /**
     * 为指定标的创建一个兑现批次。
     *
     * @param planId  计划主键
     * @param command 创建命令
     * @return 新建后的批次实体（stageStatus=PENDING_BUY）
     */
    RealizationBatch createBatch(Long planId, CreateRealizationBatchCommand command);

    /**
     * 登记一次买入操作并推进批次状态。
     *
     * @param planId  计划主键
     * @param batchId 批次主键
     * @param command 买入命令
     * @return 更新后的批次实体
     */
    RealizationBatch recordBuy(Long planId, Long batchId, RecordRealizationBuyCommand command);

    /**
     * 登记一次卖出操作并推进批次状态；卖出累计达到批次数量时进入 COMPLETED。
     *
     * @param planId  计划主键
     * @param batchId 批次主键
     * @param command 卖出命令
     * @return 更新后的批次实体
     */
    RealizationBatch recordSell(Long planId, Long batchId, RecordRealizationSellCommand command);
}
