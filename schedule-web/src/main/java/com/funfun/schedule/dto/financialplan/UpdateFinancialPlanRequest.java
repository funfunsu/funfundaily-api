package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新理财计划请求（API-3）。
 */
@Data
public class UpdateFinancialPlanRequest {

    /** 计划名称。 */
    private String planName;

    /** 计划状态。 */
    private PlanStatus status;

    /** 时间范围模式。 */
    private TimeRangeType timeRangeType;

    /** 财年。 */
    private Integer fiscalYear;

    /** 起始日期。 */
    private LocalDate startDate;

    /** 结束日期。 */
    private LocalDate endDate;

    /** 备注。 */
    private String remark;

    /** 用户设定的目标盈利。 */
    private BigDecimal targetProfit;

    /** 乐观锁版本号（必填）。 */
    private Integer version;
}
