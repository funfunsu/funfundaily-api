package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.dto.ProfitSummaryDTO;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import lombok.Data;

import java.util.List;

/**
 * 理财计划详情响应（API-9）。
 */
@Data
public class FinancialPlanDetailResponse {

    /** 计划本体。 */
    private FinancialPlan plan;

    /** 计划下的全部有效标的，按 sequenceNo 升序。 */
    private List<FinancialPlanAsset> assets;

    /** 计划下的全部有效兑现批次，按创建时间倒序。 */
    private List<RealizationBatch> realizationBatches;

    /** 计划层收益汇总。 */
    private ProfitSummaryDTO summary;
}
