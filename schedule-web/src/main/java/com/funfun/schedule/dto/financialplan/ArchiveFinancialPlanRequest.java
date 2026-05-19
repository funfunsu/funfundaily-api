package com.funfun.schedule.dto.financialplan;

import lombok.Data;

/**
 * 停用理财计划请求体（API-11 DELETE）。
 */
@Data
public class ArchiveFinancialPlanRequest {

    /** 乐观锁版本号（必填）。 */
    private Integer version;
}
