package com.funfun.schedule.repository;

import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.enums.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 兑现批次仓储。
 */
@Repository
public interface RealizationBatchRepository extends JpaRepository<RealizationBatch, Long> {

    /**
     * 查询计划下的全部有效批次。
     */
    List<RealizationBatch> findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(Long planId);

    /**
     * 查询标的下的全部有效批次。
     */
    List<RealizationBatch> findByAssetIdAndDeletedFalseOrderByCreatedAtDesc(Long assetId);

    /**
     * 按批次主键查询有效记录。
     */
    Optional<RealizationBatch> findByBatchIdAndDeletedFalse(Long batchId);

    /**
     * 聚合标的已登记批次数量。
     */
    @Query("SELECT COALESCE(SUM(rb.quantity), 0) FROM RealizationBatch rb WHERE rb.assetId = :assetId")
    BigDecimal sumBatchQuantityByAssetId(@Param("assetId") Long assetId);

    /**
     * 聚合标的已完成批次实际盈利。
     */
    @Query("SELECT COALESCE(SUM(rb.actualProfit), 0) FROM RealizationBatch rb " +
            "WHERE rb.assetId = :assetId AND rb.stageStatus = :stageStatus")
    BigDecimal sumCompletedActualProfitByAssetId(
            @Param("assetId") Long assetId,
            @Param("stageStatus") StageStatus stageStatus
    );
}
