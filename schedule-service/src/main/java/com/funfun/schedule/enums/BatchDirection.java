package com.funfun.schedule.enums;

/**
 * 衍生品批次方向。
 *
 * <ul>
 *   <li>{@code CALL} 买入看涨期权</li>
 *   <li>{@code PUT} 买入看跌期权</li>
 *   <li>{@code SHORT_CALL} 卖空看涨（卖出 call 期权）</li>
 *   <li>{@code SHORT_PUT} 卖空看跌（卖出 put 期权）</li>
 * </ul>
 */
public enum BatchDirection {
    CALL,
    PUT,
    SHORT_CALL,
    SHORT_PUT
}
