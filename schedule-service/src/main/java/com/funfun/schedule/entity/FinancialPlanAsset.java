package com.funfun.schedule.entity;

import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 理财计划标的实体。
 */
@Data
@Entity
@Table(name = "financial_plan_asset", indexes = {
        @Index(name = "uk_financial_plan_asset_plan_code_type", columnList = "plan_id,asset_code,asset_type", unique = true),
        @Index(name = "idx_financial_plan_asset_plan_id", columnList = "plan_id")
})
@Where(clause = "deleted = 0")
@SQLDelete(sql = "UPDATE financial_plan_asset SET deleted = 1 WHERE asset_id = ?")
public class FinancialPlanAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 16)
    private PlanType assetType;

    @Column(name = "asset_code", nullable = false, length = 64)
    private String assetCode;

    @Column(name = "asset_name", nullable = false, length = 128)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_sub_type", length = 16)
    private StockSubType stockSubType;

    @Column(name = "plan_buy_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal planBuyPrice;

    @Column(name = "plan_sell_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal planSellPrice;

    @Column(name = "plan_quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal planQuantity;

    @Column(name = "realized_quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal realizedQuantity = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

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
