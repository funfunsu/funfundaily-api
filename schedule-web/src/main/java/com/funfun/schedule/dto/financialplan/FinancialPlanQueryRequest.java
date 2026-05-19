package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 理财计划分页查询请求（API-1）。
 */
@Data
public class FinancialPlanQueryRequest {

    /** 群组 ID（必填，权限校验依据）。 */
    private Long groupId;

    /** 计划负责人用户 ID（可选，过滤条件）。 */
    private Long ownerUserId;

    /** 名称模糊关键字。 */
    private String keyword;

    /** 计划类型。 */
    private PlanType planType;

    /** 执行状态。 */
    private PlanStatus executionStatus;

    /** 时间范围模式。 */
    private TimeRangeType timeRangeType;

    /** 查询窗口起始日（与计划 endDate 比较）。 */
    private LocalDate startDate;

    /** 查询窗口结束日（与计划 startDate 比较）。 */
    private LocalDate endDate;

    /** 页码（1 起）。 */
    private Integer pageNo;

    /** 每页条数（默认 20，上限 200）。 */
    private Integer pageSize;
}
