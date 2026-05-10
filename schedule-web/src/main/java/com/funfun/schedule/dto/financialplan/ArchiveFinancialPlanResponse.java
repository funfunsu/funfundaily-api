package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.PlanStatus;
import lombok.Data;

/**
 * 停用理财计划响应（API-11）。
 */
@Data
public class ArchiveFinancialPlanResponse {

    /** 计划主键。 */
    private Long planId;

    /** 归档后的计划状态。 */
    private PlanStatus status;
}
