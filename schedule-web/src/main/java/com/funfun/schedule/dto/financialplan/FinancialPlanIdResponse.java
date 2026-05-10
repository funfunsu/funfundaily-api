package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.PlanStatus;
import lombok.Data;

/**
 * 理财计划写入路径通用响应（API-2 / API-3）。
 */
@Data
public class FinancialPlanIdResponse {

    /** 计划主键。 */
    private Long planId;

    /** 计划状态。 */
    private PlanStatus status;

    /** 乐观锁版本号。 */
    private Integer version;
}
