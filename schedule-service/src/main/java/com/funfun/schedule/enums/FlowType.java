package com.funfun.schedule.enums;

/**
 * 流水类型，与 transaction_flow.flow_type 列对齐：
 *   POINTS = 0  积分
 *   CASH   = 1  现金
 * 注：用 ORDINAL 持久化，所以**不要随意调换枚举顺序**。
 */
public enum FlowType {
    POINTS,
    CASH
}
