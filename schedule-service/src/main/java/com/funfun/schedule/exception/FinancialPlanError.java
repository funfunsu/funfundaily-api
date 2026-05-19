package com.funfun.schedule.exception;

/**
 * 理财计划领域错误码常量。
 *
 * <p>错误码字符串对应 docs/design.md 中定义的业务错误码，便于前后端契约一致。
 */
public final class FinancialPlanError {

    private FinancialPlanError() {
    }

    public static final CommonException FP_PERMISSION_DENIED =
            new CommonException("FP_PERMISSION_DENIED", "无权访问理财计划.{0}");

    public static final CommonException FP_VALIDATION_FAILED =
            new CommonException("FP_VALIDATION_FAILED", "理财计划参数校验失败.{0}");

    public static final CommonException FP_WINDOW_INVALID =
            new CommonException("FP_WINDOW_INVALID", "理财计划时间窗口非法.{0}");

    public static final CommonException FP_TYPE_UNSUPPORTED =
            new CommonException("FP_TYPE_UNSUPPORTED", "理财计划类型不支持.{0}");

    public static final CommonException FP_PLAN_NOT_FOUND =
            new CommonException("FP_PLAN_NOT_FOUND", "理财计划不存在.{0}");

    public static final CommonException FP_VERSION_CONFLICT =
            new CommonException("FP_VERSION_CONFLICT", "理财计划版本冲突.{0}");

    public static final CommonException FP_ASSET_INVALID =
            new CommonException("FP_ASSET_INVALID", "计划标的参数非法.{0}");

    public static final CommonException FP_ASSET_DUPLICATED =
            new CommonException("FP_ASSET_DUPLICATED", "同一计划下标的重复.{0}");

    public static final CommonException FP_ASSET_NOT_FOUND =
            new CommonException("FP_ASSET_NOT_FOUND", "计划标的不存在.{0}");

    public static final CommonException FP_ASSET_QTY_LT_REALIZED =
            new CommonException("FP_ASSET_QTY_LT_REALIZED", "计划数量不得小于已兑现数量.{0}");

    public static final CommonException FP_REALIZATION_QTY_EXCEEDED =
            new CommonException("FP_REALIZATION_QTY_EXCEEDED", "兑现数量超出可兑现额度.{0}");

    public static final CommonException FP_BATCH_NOT_FOUND =
            new CommonException("FP_BATCH_NOT_FOUND", "兑现批次不存在.{0}");

    public static final CommonException FP_SELL_BEFORE_BUY =
            new CommonException("FP_SELL_BEFORE_BUY", "卖出登记需先存在买入记录.{0}");

    public static final CommonException FP_STAGE_CONFLICT =
            new CommonException("FP_STAGE_CONFLICT", "兑现批次状态非法迁移.{0}");

    public static final CommonException FP_PLAN_ALREADY_ARCHIVED =
            new CommonException("FP_PLAN_ALREADY_ARCHIVED", "理财计划已归档不可编辑.{0}");

    public static final CommonException FP_QUERY_INVALID =
            new CommonException("FP_QUERY_INVALID", "理财计划查询参数非法.{0}");

    public static final CommonException FP_STAT_CALC_FAILED =
            new CommonException("FP_STAT_CALC_FAILED", "理财计划统计计算失败.{0}");
}
