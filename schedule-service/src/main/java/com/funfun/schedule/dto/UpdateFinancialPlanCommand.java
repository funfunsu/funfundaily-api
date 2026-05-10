package com.funfun.schedule.dto;

import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新理财计划命令对象。
 *
 * <p>对应 API-3 更新计划，包含基础信息、时间窗口、状态与乐观锁版本号。
 */
@Data
public class UpdateFinancialPlanCommand {

    /** 计划名称。 */
    private String planName;

    /** 计划状态，可由 ACTIVE 切换为 ARCHIVED 等。 */
    private PlanStatus status;

    /** 时间范围模式。 */
    private TimeRangeType timeRangeType;

    /** 财年（自然年）。 */
    private Integer fiscalYear;

    /** 自定义起始日期。 */
    private LocalDate startDate;

    /** 自定义结束日期。 */
    private LocalDate endDate;

    /** 备注。 */
    private String remark;

    /** 乐观锁版本号。 */
    private Integer version;
}
