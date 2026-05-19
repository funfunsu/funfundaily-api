package com.funfun.schedule.service;

import com.funfun.schedule.dto.SaveFinancialPlanAssetItem;
import com.funfun.schedule.dto.UpdateFinancialPlanAssetParamsCommand;
import com.funfun.schedule.entity.FinancialPlanAsset;

import java.util.List;

/**
 * 计划标的领域服务。
 *
 * <p>承载计划标的的批量保存、参数调整与查询逻辑；
 * 严格遵循 INV-2（数量正且不小于已兑现）、唯一性约束以及 INV-7（归档不可编辑）等不变量。
 */
public interface PlanAssetService {

    /**
     * 批量保存计划标的（新增 + 更新）。
     *
     * @param planId 计划主键
     * @param items  标的项集合
     * @return 保存后的标的列表（按 sequenceNo 升序）
     */
    List<FinancialPlanAsset> saveAssets(Long planId, List<SaveFinancialPlanAssetItem> items);

    /**
     * 调整单个标的的计划参数（价格、数量）。
     *
     * @param planId   计划主键
     * @param assetId  标的主键
     * @param command  调整命令
     * @return 调整后的标的实体
     */
    FinancialPlanAsset updateAssetParams(Long planId,
                                         Long assetId,
                                         UpdateFinancialPlanAssetParamsCommand command);

    /**
     * 查询计划下全部有效标的。
     *
     * @param planId 计划主键
     * @return 标的列表
     */
    List<FinancialPlanAsset> listAssetsByPlanId(Long planId);

    /**
     * 按主键查询单个有效标的，未找到时抛 FP_ASSET_NOT_FOUND。
     *
     * @param assetId 标的主键
     * @return 标的实体
     */
    FinancialPlanAsset getAsset(Long assetId);
}
