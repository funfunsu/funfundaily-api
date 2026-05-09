package com.funfun.schedule.enums;

/**
 * 交易类型，与 transaction_flow.transaction_type 列对齐：
 *   1 = INCOME   收入
 *   2 = EXPENSE  支出
 * 用自定义 converter 把 1/2 ↔ INCOME/EXPENSE。
 */
public enum TransactionType {
    INCOME(1),
    EXPENSE(2);

    private final int code;

    TransactionType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TransactionType fromCode(int code) {
        for (TransactionType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown TransactionType code: " + code);
    }
}
