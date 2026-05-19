package com.funfun.schedule.dto;

import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 理财计划查询命令对象。
 *
 * <p>对应 API-1 分页查询，所有字段均为可选过滤条件。
 */
@Data
public class QueryFinancialPlanCommand {

    /** 所属群组（家庭）ID。 */
    private Long groupId;

    /** 计划负责人用户 ID。 */
    private Long ownerUserId;

    /** 名称模糊关键字。 */
    private String keyword;

    /** 计划类型。 */
    private PlanType planType;

    /** 执行状态（DRAFT / ACTIVE / ARCHIVED）。 */
    private PlanStatus executionStatus;

    /** 时间范围模式。 */
    private TimeRangeType timeRangeType;

    /** 查询窗口起始日（与计划 endDate 比较）。 */
    private LocalDate startDate;

    /** 查询窗口结束日（与计划 startDate 比较）。 */
    private LocalDate endDate;

    /** 页码（从 1 开始）。 */
    private Integer pageNo;

    /** 每页条数。 */
    private Integer pageSize;
}
