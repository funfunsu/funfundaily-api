package com.funfun.schedule.entity;

import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.enums.TimeRangeType;
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
 * 理财计划实体。
 */
@Data
@Entity
@Table(name = "financial_plan", indexes = {
        @Index(name = "idx_financial_plan_group_status", columnList = "group_id,status"),
        @Index(name = "idx_financial_plan_owner_status", columnList = "owner_user_id,status"),
        @Index(name = "idx_financial_plan_fiscal_year", columnList = "fiscal_year")
})
@Where(clause = "deleted = 0")
@SQLDelete(sql = "UPDATE financial_plan SET deleted = 1 WHERE plan_id = ?")
public class FinancialPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "plan_name", nullable = false, length = 128)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 16)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_sub_type", length = 16)
    private StockSubType stockSubType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_range_type", nullable = false, length = 16)
    private TimeRangeType timeRangeType;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "remark", length = 1024)
    private String remark;

    /** 用户在计划层设定的目标盈利；计划/实际完成度均按这个数计算。 */
    @Column(name = "target_profit", nullable = false, precision = 24, scale = 8)
    private BigDecimal targetProfit;

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
