package com.funfun.schedule.enums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 交易类型，与 transaction_flow.transaction_type 列对齐：
 *   1 = INCOME   收入
 *   2 = EXPENSE  支出
 * 用自定义 converter 把 1/2 ↔ INCOME/EXPENSE。
 */
public enum TransactionType {
    INCOME(1),
    EXPENSE(2);

    private static final Logger log = LoggerFactory.getLogger(TransactionType.class);

    private final int code;

    TransactionType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 容错：遇到历史脏数据（例如 transaction_type=0）时返回 null，避免读取链路炸掉。
     * 调用方按 null 处理（balance 查询等只关心金额，不依赖该字段）。
     */
    public static TransactionType fromCode(int code) {
        for (TransactionType t : values()) {
            if (t.code == code) return t;
        }
        log.warn("Unknown TransactionType code: {}, return null", code);
        return null;
    }
}
