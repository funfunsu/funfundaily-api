package com.funfun.schedule.dto;

import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.enums.TimeRangeType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建理财计划命令对象。
 *
 * <p>对应 API-2 创建计划，由 schedule-web 控制器层映射后传入服务层。
 */
@Data
public class CreateFinancialPlanCommand {

    /** 所属群组（家庭）ID。 */
    private Long groupId;

    /** 计划负责人用户 ID。 */
    private Long ownerUserId;

    /** 计划名称。 */
    private String planName;

    /** 计划类型：SAVINGS / STOCK。 */
    private PlanType planType;

    /** 股票计划子类型：EQUITY / OPTION，仅 planType=STOCK 时有意义。 */
    private StockSubType stockSubType;

    /** 时间范围模式：YEAR / CUSTOM。 */
    private TimeRangeType timeRangeType;

    /** 财年（自然年），timeRangeType=YEAR 时必填。 */
    private Integer fiscalYear;

    /** 自定义起始日期，timeRangeType=CUSTOM 时必填。 */
    private LocalDate startDate;

    /** 自定义结束日期，timeRangeType=CUSTOM 时必填。 */
    private LocalDate endDate;

    /** 备注。 */
    private String remark;

    /** 用户设定的目标盈利。 */
    private BigDecimal targetProfit;
}
