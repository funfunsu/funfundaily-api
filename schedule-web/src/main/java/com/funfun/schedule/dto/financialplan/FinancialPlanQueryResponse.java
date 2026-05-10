package com.funfun.schedule.dto.financialplan;

import lombok.Data;

import java.util.List;

/**
 * 理财计划分页查询响应（API-1）。
 */
@Data
public class FinancialPlanQueryResponse {

    /** 当前页数据。 */
    private List<FinancialPlanListItemDTO> list;

    /** 总条数。 */
    private long total;

    /** 当前页码（1 起）。 */
    private int pageNo;

    /** 每页条数。 */
    private int pageSize;
}
