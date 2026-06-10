package com.funfun.schedule.dto.financialplan;

import com.funfun.schedule.enums.ExerciseAction;
import com.funfun.schedule.enums.OptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 行权 / 被行权请求：针对批次内某个期权 key 执行。
 */
@Data
public class ExerciseOptionRequest {

    /** 期权类型：CALL / PUT。 */
    private OptionType optionType;

    /** 目标价格（行权价）。 */
    private BigDecimal strikePrice;

    /** 到期时间。 */
    private LocalDate expirationDate;

    /** EXERCISE（行权）/ ASSIGN（被行权）。 */
    private ExerciseAction action;
}
