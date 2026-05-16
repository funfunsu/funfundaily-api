package com.funfun.schedule.dto;

import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import lombok.Data;

import java.util.List;

/**
 * 理财计划详情聚合 DTO。
 *
 * <p>对应 API-9 返回的 plan / assets / realizationBatches 三段聚合数据；
 * 统计快照 summary 由统计服务（任务 2.4）单独装配。
 */
@Data
public class FinancialPlanDetailDTO {

    /** 理财计划本体。 */
    private FinancialPlan plan;

    /** 计划下的全部有效标的，按 sequenceNo 升序。 */
    private List<FinancialPlanAsset> assets;

    /** 计划下的全部有效兑现批次，按创建时间倒序。 */
    private List<RealizationBatch> realizationBatches;
}
