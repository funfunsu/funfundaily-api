package com.funfun.schedule.entity;

import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OperationType;
import com.funfun.schedule.enums.OptionType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 兑现操作明细实体。
 *
 * <p>同一批次（=一只正股）下可记录正股(STOCK)与期权(OPTION)两类操作。
 * 期权额外携带 optionType / strikePrice / expirationDate，三者构成「期权 key」。
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

    /** STOCK（正股）/ OPTION（期权）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "instrument", nullable = false, length = 8)
    private InstrumentType instrument = InstrumentType.STOCK;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 8)
    private OperationType operationType;

    /** 仅 OPTION 有效：CALL / PUT。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", length = 8)
    private OptionType optionType;

    /** 仅 OPTION 有效：目标价格（行权价）。 */
    @Column(name = "strike_price", precision = 20, scale = 2)
    private BigDecimal strikePrice;

    /** 仅 OPTION 有效：到期时间。 */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

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
