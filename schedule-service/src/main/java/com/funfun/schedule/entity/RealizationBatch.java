package com.funfun.schedule.entity;

import com.funfun.schedule.enums.BatchDirection;
import com.funfun.schedule.enums.BatchType;
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
 *
 * <p>新模型：批次承载具体的「数量 + 计划买入价 + 计划卖出价 + 类型」三件套；
 * EQUITY = 正股，DERIVATIVE = 衍生品（同时携带 direction 与 expirationDate）。
 * 实际买卖明细记录在 {@link RealizationOperation}，本表上的 actualX/feeTotal/stageStatus
 * 是写入时的聚合视图，便于查询/统计无需 join 每条操作。
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

    @Column(name = "batch_name", length = 128)
    private String batchName;

    /** EQUITY / DERIVATIVE。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", nullable = false, length = 16)
    private BatchType batchType;

    /** 仅 DERIVATIVE 有效：CALL / PUT / SHORT_CALL / SHORT_PUT。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 16)
    private BatchDirection direction;

    /** 计划数量（合约数 / 股数）。 */
    @Column(name = "quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    /** 计划买入价。 */
    @Column(name = "plan_buy_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal planBuyPrice;

    /** 计划卖出价。 */
    @Column(name = "plan_sell_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal planSellPrice;

    /** 仅 DERIVATIVE 有效：到期日。 */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

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
