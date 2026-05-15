package com.funfun.schedule.repository;

import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.enums.AssetMarket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 理财计划标的仓储。
 */
@Repository
public interface FinancialPlanAssetRepository extends JpaRepository<FinancialPlanAsset, Long> {

    /**
     * 查询计划下的全部有效标的。
     */
    List<FinancialPlanAsset> findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(Long planId);

    /**
     * 按标的主键查询有效记录。
     */
    Optional<FinancialPlanAsset> findByAssetIdAndDeletedFalse(Long assetId);

    /**
     * 按 (plan, stockName, market) 唯一组合查询；用于新增前的去重判断。
     */
    Optional<FinancialPlanAsset> findByPlanIdAndStockNameAndMarketAndDeletedFalse(
            Long planId,
            String stockName,
            AssetMarket market
    );
}
