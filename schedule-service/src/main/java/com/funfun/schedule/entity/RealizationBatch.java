package com.funfun.schedule.entity;

import com.funfun.schedule.enums.StageStatus;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 兑现批次实体。
 */
@Data
@Entity
@Table(name = "realization_batch", indexes = {
        @Index(name = "idx_realization_batch_asset_stage", columnList = "asset_id,stage_status"),
        @Index(name = "idx_realization_batch_plan_id", columnList = "plan_id")
})
@Where(clause = "deleted = 0")
@SQLDelete(sql = "UPDATE realization_batch SET deleted = 1 WHERE batch_id = ?")
public class RealizationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "batch_name", nullable = false, length = 128)
    private String batchName;

    @Column(name = "quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_status", nullable = false, length = 20)
    private StageStatus stageStatus;

    @Column(name = "actual_buy_price", precision = 20, scale = 8)
    private BigDecimal actualBuyPrice;

    @Column(name = "actual_sell_price", precision = 20, scale = 8)
    private BigDecimal actualSellPrice;

    @Column(name = "actual_buy_amount", precision = 24, scale = 8)
    private BigDecimal actualBuyAmount;

    @Column(name = "actual_sell_amount", precision = 24, scale = 8)
    private BigDecimal actualSellAmount;

    @Column(name = "actual_profit", precision = 24, scale = 8)
    private BigDecimal actualProfit;

    @Column(name = "buy_trade_date")
    private LocalDate buyTradeDate;

    @Column(name = "sell_trade_date")
    private LocalDate sellTradeDate;

    @Column(name = "fee_total", nullable = false, precision = 24, scale = 8)
    private BigDecimal feeTotal = BigDecimal.ZERO;

    @Column(name = "note", length = 1024)
    private String note;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
