package com.funfun.schedule.service;

import com.funfun.schedule.dto.AssetProfitSummaryDTO;
import com.funfun.schedule.dto.ProfitSummaryDTO;
import com.funfun.schedule.dto.ProgressSnapshotDTO;

import java.util.List;

/**
 * 理财计划统计与看板服务。
 *
 * <p>提供目标盈利（targetProfit）、实际盈利（actualProfit）、完成度与
 * {@link ProgressSnapshotDTO} 计算能力，遵循以下不变量：
 * <ul>
 *   <li>INV-5：targetProfit 仅依赖计划价格和计划数量，与兑现状态无关；
 *       actualProfit 仅聚合 stageStatus=COMPLETED 的批次。</li>
 *   <li>INV-6：计划层指标等于全部标的指标的聚合。</li>
 * </ul>
 */
public interface FinancialPlanStatsService {

    /**
     * 计算计划维度收益汇总。
     *
     * @param planId 计划主键
     * @return 计划维度收益汇总 DTO
     */
    ProfitSummaryDTO calcPlanSummary(Long planId);

    /**
     * 计算各标的维度收益汇总列表（顺序与标的 sequenceNo 一致）。
     *
     * @param planId 计划主键
     * @return 标的维度收益汇总列表
     */
    List<AssetProfitSummaryDTO> calcAssetSummaries(Long planId);

    /**
     * 计算计划执行进度快照（时间 / 数量 / 收益进度率及告警标志）。
     *
     * @param planId 计划主键
     * @return 执行进度快照
     */
    ProgressSnapshotDTO calcProgressSnapshot(Long planId);
}
