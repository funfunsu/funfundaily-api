package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.dto.AssetProfitSummaryDTO;
import com.funfun.schedule.dto.ProfitSummaryDTO;
import com.funfun.schedule.dto.ProgressSnapshotDTO;
import lombok.Data;

import java.util.List;

/**
 * 理财计划看板响应（API-10）。
 */
@Data
public class FinancialPlanDashboardResponse {

    /** 计划层收益汇总。 */
    private ProfitSummaryDTO planSummary;

    /** 标的维度收益汇总列表。 */
    private List<AssetProfitSummaryDTO> assetSummaries;

    /** 进度快照。 */
    private ProgressSnapshotDTO progress;
}
