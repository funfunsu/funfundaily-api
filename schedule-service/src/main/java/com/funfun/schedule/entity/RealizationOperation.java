package com.funfun.schedule.entity;

import com.funfun.schedule.enums.OperationType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 兑现操作明细实体。
 */
@Data
@Entity
@Table(name = "realization_operation", indexes = {
        @Index(name = "idx_realization_operation_batch_type", columnList = "batch_id,operation_type")
})
public class RealizationOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Long operationId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 8)
    private OperationType operationType;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "price", nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(name = "fee", nullable = false, precision = 24, scale = 8)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "note", length = 1024)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
