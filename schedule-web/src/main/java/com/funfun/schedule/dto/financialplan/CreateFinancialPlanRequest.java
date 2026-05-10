package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建理财计划请求（API-2）。
 */
@Data
public class CreateFinancialPlanRequest {

    /** 群组 ID（必填）。 */
    private Long groupId;

    /** 计划负责人用户 ID（必填，可由系统补默认）。 */
    private Long ownerUserId;

    /** 计划名称（必填）。 */
    private String planName;

    /** 计划类型（必填）。 */
    private PlanType planType;

    /** 股票子类型，仅 planType=STOCK 时生效。 */
    private StockSubType stockSubType;

    /** 时间范围模式（必填）。 */
    private TimeRangeType timeRangeType;

    /** 财年，timeRangeType=YEAR 时必填。 */
    private Integer fiscalYear;

    /** 起始日期，timeRangeType=CUSTOM 时必填。 */
    private LocalDate startDate;

    /** 结束日期，timeRangeType=CUSTOM 时必填。 */
    private LocalDate endDate;

    /** 备注。 */
    private String remark;
}
