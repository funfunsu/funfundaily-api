package com.funfun.schedule.service.support;

import com.funfun.schedule.dto.BatchStatsDTO;
import com.funfun.schedule.dto.OptionKeyStatsDTO;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OptionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 由操作明细计算批次卡片汇总数据（正股 + 各期权 key）。
 */
public final class BatchStatsCalculator {

    /** 金额输出统一保留 2 位小数。 */
    public static final int MONEY_SCALE = 2;

    private BatchStatsCalculator() {
    }

    /**
     * 计算单个批次的卡片汇总。
     *
     * @param batch          批次（用于目标收益）
     * @param opsInTimeOrder 该批次全部操作，已按 tradeDate、createdAt 升序
     */
    public static BatchStatsDTO computeStats(RealizationBatch batch, List<RealizationOperation> opsInTimeOrder) {
        BatchStatsDTO dto = new BatchStatsDTO();
        dto.setBatchId(batch.getBatchId());
        dto.setTargetProfit(round(computeTargetProfit(batch)));

        List<RealizationOperation> stockOps = new ArrayList<>();
        Map<String, List<RealizationOperation>> optionGroups = new LinkedHashMap<>();
        for (RealizationOperation op : opsInTimeOrder) {
            if (op.getInstrument() == InstrumentType.OPTION) {
                optionGroups.computeIfAbsent(optionKeyOf(op), k -> new ArrayList<>()).add(op);
            } else {
                stockOps.add(op);
            }
        }

        PositionCalculator.Result stock = PositionCalculator.compute(stockOps);
        dto.setStockQuantity(stock.getNetQuantity());
        dto.setStockCostPrice(stock.getAvgCost() == null ? null : round(stock.getAvgCost()));
        dto.setStockCostAmount(round(stock.getCostAmount()));
        dto.setStockRealizedProfit(round(stock.getRealizedProfit()));

        BigDecimal optionRealized = BigDecimal.ZERO;
        List<OptionKeyStatsDTO> keys = new ArrayList<>();
        for (List<RealizationOperation> group : optionGroups.values()) {
            RealizationOperation head = group.get(0);
            PositionCalculator.Result r = PositionCalculator.compute(group);

            OptionKeyStatsDTO k = new OptionKeyStatsDTO();
            k.setOptionType(head.getOptionType());
            k.setStrikePrice(head.getStrikePrice());
            k.setExpirationDate(head.getExpirationDate());
            k.setNetQuantity(r.getNetQuantity());
            k.setCostAmount(round(r.getCostAmount()));
            k.setAvgCost(r.getAvgCost() == null ? null : round(r.getAvgCost()));
            k.setRealizedProfit(round(r.getRealizedProfit()));
            keys.add(k);

            optionRealized = optionRealized.add(r.getRealizedProfit());
        }
        keys.sort(Comparator
                .comparing(OptionKeyStatsDTO::getExpirationDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(k -> k.getOptionType() == null ? "" : k.getOptionType().name())
                .thenComparing(OptionKeyStatsDTO::getStrikePrice, Comparator.nullsLast(BigDecimal::compareTo)));
        dto.setOptionKeys(keys);

        dto.setOptionRealizedProfit(round(optionRealized));
        dto.setTotalRealizedProfit(round(stock.getRealizedProfit().add(optionRealized)));
        return dto;
    }

    /** 目标收益 = (计划卖出价 - 计划买入价) × 计划数量。 */
    public static BigDecimal computeTargetProfit(RealizationBatch batch) {
        BigDecimal buy = batch.getPlanBuyPrice();
        BigDecimal sell = batch.getPlanSellPrice();
        BigDecimal qty = batch.getQuantity();
        if (buy == null || sell == null || qty == null) {
            return BigDecimal.ZERO;
        }
        return sell.subtract(buy).multiply(qty);
    }

    /** 期权 key 规范化字符串：optionType|strike(2)|expiration。 */
    public static String optionKeyOf(RealizationOperation op) {
        OptionType type = op.getOptionType();
        BigDecimal strike = op.getStrikePrice();
        LocalDate exp = op.getExpirationDate();
        return (type == null ? "?" : type.name())
                + "|" + (strike == null ? "?" : strike.setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString())
                + "|" + (exp == null ? "?" : exp.toString());
    }

    private static BigDecimal round(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
