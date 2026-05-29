package com.funfun.schedule.enums;

/**
 * 兑现操作标的类型。
 *
 * <ul>
 *   <li>{@code STOCK} 正股</li>
 *   <li>{@code OPTION} 期权（衍生品，作为批次内的操作记录）</li>
 * </ul>
 */
public enum InstrumentType {
    STOCK,
    OPTION
}
