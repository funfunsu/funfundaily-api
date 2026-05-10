package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.dto.SaveFinancialPlanAssetItem;
import lombok.Data;

import java.util.List;

/**
 * 批量保存计划标的请求（API-4）。
 */
@Data
public class SaveFinancialPlanAssetsRequest {

    /** 标的项列表（不可为空）。 */
    private List<SaveFinancialPlanAssetItem> items;
}
