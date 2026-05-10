package com.funfun.schedule.repository;

import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.TimeRangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 理财计划仓储。
 */
@Repository
public interface FinancialPlanRepository extends JpaRepository<FinancialPlan, Long> {

    /**
     * 按条件分页查询理财计划。
     */
    @Query("SELECT p FROM FinancialPlan p WHERE " +
            "(:groupId IS NULL OR p.groupId = :groupId) AND " +
            "(:keyword IS NULL OR p.planName LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:planType IS NULL OR p.planType = :planType) AND " +
            "(:timeRangeType IS NULL OR p.timeRangeType = :timeRangeType) AND " +
            "(:windowStart IS NULL OR p.endDate >= :windowStart) AND " +
            "(:windowEnd IS NULL OR p.startDate <= :windowEnd)")
    Page<FinancialPlan> queryByConditions(
            @Param("groupId") Long groupId,
            @Param("keyword") String keyword,
            @Param("status") PlanStatus status,
            @Param("planType") PlanType planType,
            @Param("timeRangeType") TimeRangeType timeRangeType,
            @Param("windowStart") LocalDate windowStart,
            @Param("windowEnd") LocalDate windowEnd,
            Pageable pageable
    );

    /**
     * 按计划主键查询有效记录。
     */
    Optional<FinancialPlan> findByPlanIdAndDeletedFalse(Long planId);
}
