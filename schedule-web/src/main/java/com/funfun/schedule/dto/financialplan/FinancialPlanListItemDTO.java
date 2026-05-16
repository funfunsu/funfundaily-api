package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 理财计划列表项（API-1 list 元素）。
 */
@Data
public class FinancialPlanListItemDTO {

    /** 计划主键。 */
    private Long planId;

    /** 群组 ID。 */
    private Long groupId;

    /** 计划负责人用户 ID。 */
    private Long ownerUserId;

    /** 计划名称。 */
    private String planName;

    /** 计划类型。 */
    private PlanType planType;

    /** 股票子类型。 */
    private StockSubType stockSubType;

    /** 计划状态。 */
    private PlanStatus status;

    /** 时间范围模式。 */
    private TimeRangeType timeRangeType;

    /** 财年。 */
    private Integer fiscalYear;

    /** 开始日期。 */
    private LocalDate startDate;

    /** 结束日期。 */
    private LocalDate endDate;

    /** 备注。 */
    private String remark;

    /** 乐观锁版本号。 */
    private Integer version;
}
