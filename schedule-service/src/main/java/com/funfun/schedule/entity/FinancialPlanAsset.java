package com.funfun.schedule.entity;

import com.funfun.schedule.enums.AssetMarket;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 理财计划标的实体。
 *
 * <p>新模型：仅描述一只股票及其目标利润，具体的买入/卖出/数量/到期信息下移到批次层。
 */
@Data
@Entity
@Table(name = "financial_plan_asset", indexes = {
        @Index(name = "uk_financial_plan_asset_plan_stock_market",
                columnList = "plan_id,stock_name,market", unique = true),
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

    /** 股票名称（A 股/港股/美股的可读名）。 */
    @Column(name = "stock_name", nullable = false, length = 128)
    private String stockName;

    /** 所属市场：US / HK / CN。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "market", nullable = false, length = 8)
    private AssetMarket market;

    /** 用户设定的目标盈利（与各批次的目标收益独立）。 */
    @Column(name = "target_profit", nullable = false, precision = 24, scale = 8)
    private BigDecimal targetProfit;

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
