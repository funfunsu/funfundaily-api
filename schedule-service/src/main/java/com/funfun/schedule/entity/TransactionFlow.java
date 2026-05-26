package com.funfun.schedule.entity;

import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.TransactionType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 交易流水实体，对应 transaction_flow 表。
 *
 * - flow_type 列是 TINYINT，由 FlowType 用 ORDINAL 序列化（POINTS=0 / CASH=1）。
 * - transaction_type 列是 TINYINT，由 TransactionTypeConverter 处理 1=INCOME / 2=EXPENSE。
 */
@Data
@Entity
@Table(name = "transaction_flow")
public class TransactionFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Convert(converter = TransactionTypeConverter.class)
    @Column(name = "transaction_type", nullable = false, columnDefinition = "TINYINT")
    private TransactionType transactionType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "flow_type", nullable = false, columnDefinition = "TINYINT")
    private FlowType flowType;

    @Column(name = "amount", nullable = false)
    private Integer amount = 0;

    @Column(name = "balance", nullable = false)
    private Integer balance = 0;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra;

    @Column(name = "operator", nullable = false)
    private Long operator;

    /** Hibernate 在 INSERT 时自动填值。不靠 DB 的 DEFAULT CURRENT_TIMESTAMP，这样 H2/MySQL 都正确。 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Hibernate 在 INSERT/UPDATE 时自动维护。 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 把 1/2 ↔ TransactionType 的 JPA Converter，挂在 transactionType 字段上。
     */
    @Converter
    public static class TransactionTypeConverter implements AttributeConverter<TransactionType, Integer> {
        @Override
        public Integer convertToDatabaseColumn(TransactionType attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public TransactionType convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : TransactionType.fromCode(dbData);
        }
    }
}
