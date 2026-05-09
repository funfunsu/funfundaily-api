package com.funfun.schedule.dto.point;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兑换记录视图。
 * 数据底座：transaction_flow（FlowType=POINTS, TransactionType=EXPENSE）。
 * productName / productId 来自流水的 extra JSON。
 */
@Data
public class PointExchangeRecordItem {
    /** transaction_flow 的 id */
    private Long id;
    private Long userId;
    private Long groupId;
    private Long productId;
    private String productName;
    /** 本次扣减积分（正数） */
    private Integer scoreDeducted;
    /** 扣减后余额（来自 transaction_flow.balance） */
    private Integer balanceAfter;
    /** 兑换时间（来自 transaction_flow.created_at） */
    private LocalDateTime exchangeTime;
}
