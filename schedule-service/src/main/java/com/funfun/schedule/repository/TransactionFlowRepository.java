package com.funfun.schedule.repository;

import com.funfun.schedule.entity.TransactionFlow;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionFlowRepository extends JpaRepository<TransactionFlow, Long> {

    /** 取该用户该 flowType 最近一条流水（用来读余额） */
    Optional<TransactionFlow> findFirstByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(
            Long groupId, Long userId, FlowType flowType);

    /** 全量列表（无分页） */
    List<TransactionFlow> findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(
            Long groupId, Long userId, FlowType flowType);

    /** 分页 */
    Page<TransactionFlow> findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(
            Long groupId, Long userId, FlowType flowType, Pageable pageable);

    /** 时间范围内分页 */
    Page<TransactionFlow> findByGroupIdAndUserIdAndFlowTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long groupId, Long userId, FlowType flowType,
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 按 group + flowType + transactionType + 时间范围分页。userId 可空（家长查群组所有成员）。
     * 主要给"积分兑换记录"页用：flowType=POINTS, transactionType=EXPENSE。
     */
    @Query("SELECT t FROM TransactionFlow t WHERE t.groupId = :groupId " +
            "AND (:userId IS NULL OR t.userId = :userId) " +
            "AND t.flowType = :flowType " +
            "AND t.transactionType = :txnType " +
            "AND (:startTime IS NULL OR t.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR t.createdAt < :endTime) " +
            "ORDER BY t.createdAt DESC")
    Page<TransactionFlow> queryFlowsByType(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("flowType") FlowType flowType,
            @Param("txnType") TransactionType txnType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}
