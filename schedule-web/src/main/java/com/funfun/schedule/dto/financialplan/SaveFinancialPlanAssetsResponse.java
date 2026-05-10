package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.entity.FinancialPlanAsset;
import lombok.Data;

import java.util.List;

/**
 * 批量保存计划标的响应（API-4）。
 */
@Data
public class SaveFinancialPlanAssetsResponse {

    /** 计划主键。 */
    private Long planId;

    /** 保存后的全部标的列表（按 sequenceNo 升序）。 */
    private List<FinancialPlanAsset> items;
}
