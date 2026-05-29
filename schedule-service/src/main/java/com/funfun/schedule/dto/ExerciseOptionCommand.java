package com.funfun.schedule.dto;

import com.funfun.schedule.enums.ExerciseAction;
import com.funfun.schedule.enums.OptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 行权 / 被行权命令：针对批次内某个期权 key 执行。
 *
 * <p>行权数量取该 key 的净持仓绝对值；期权按价格 0 平仓，并自动建一条正股记录
 * （价格 = strikePrice，数量 = 净持仓绝对值，1:1 映射）。
 */
@Data
public class ExerciseOptionCommand {

    /** 期权类型：CALL / PUT。 */
    private OptionType optionType;

    /** 目标价格（行权价）。 */
    private BigDecimal strikePrice;

    /** 到期时间。 */
    private LocalDate expirationDate;

    /** EXERCISE（行权）/ ASSIGN（被行权）。 */
    private ExerciseAction action;
}
