package com.funfun.schedule.enums;

/**
 * 期权行权动作。
 *
 * <ul>
 *   <li>{@code EXERCISE} 行权：净持仓多头(>0)时主动行权，期权按卖价 0 平仓，
 *       并自动建一条正股记录（CALL→买入，PUT→卖出）。</li>
 *   <li>{@code ASSIGN} 被行权：净持仓空头(&lt;0)时被指派，期权按买价 0 平仓，
 *       并自动建一条正股记录（CALL→卖出，PUT→买入）。</li>
 * </ul>
 */
public enum ExerciseAction {
    EXERCISE,
    ASSIGN
}
