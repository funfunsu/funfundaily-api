package com.funfun.schedule.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 计划执行进度快照 DTO（对应 design.md § 3.2 ProgressSnapshot）。
 *
 * <p>各进度率均为 [0, 1] 区间小数（可超过 1.0 以表达超额完成或超期）。
 */
@Data
public class ProgressSnapshotDTO {

    /**
     * 计划当前状态：
     * NOT_STARTED / IN_PROGRESS / COMPLETED / PARTIAL
     */
    private String planStatus;

    /**
     * 时间进度率 = (today − startDate) / (endDate − startDate)。
     * startDate == endDate 时置 1；today < startDate 时置 0。
     */
    private BigDecimal timeProgressRate;

    /**
     * 数量进度率 = realizedQuantity / plannedQuantity。
     * plannedQuantity=0 时置 0。
     */
    private BigDecimal quantityProgressRate;

    /**
     * 收益进度率 = actualProfit / targetProfit。
     * targetProfit=0 时置 0。
     */
    private BigDecimal profitProgressRate;

    /**
     * 告警标志列表；可能包含：
     * <ul>
     *   <li>OVER_WINDOW：已超出计划结束日期</li>
     *   <li>INCOMPLETE_BATCH：存在未完成兑现批次</li>
     *   <li>QUANTITY_REACHED：已兑现数量已达到计划数量</li>
     * </ul>
     */
    private List<String> warningFlags = new ArrayList<>();
}
