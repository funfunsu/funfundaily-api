package com.funfun.schedule.repository;

import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 兑现操作明细仓储。
 */
@Repository
public interface RealizationOperationRepository extends JpaRepository<RealizationOperation, Long> {

    /**
     * 查询批次下按时间排序的操作流水。
     */
    List<RealizationOperation> findByBatchIdOrderByTradeDateAscCreatedAtAsc(Long batchId);

    /**
     * 查询批次下指定操作类型流水。
     */
    List<RealizationOperation> findByBatchIdAndOperationTypeOrderByTradeDateAscCreatedAtAsc(
            Long batchId,
            OperationType operationType
    );
}
