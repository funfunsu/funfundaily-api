package com.funfun.schedule.service.support;

import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.OperationType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 单一「持仓 key」（正股，或某个期权 key）的带符号加权平均成本（WAC）计算器。
 *
 * <p>支持多头与空头、以及方向翻转：
 * <ul>
 *   <li>BUY → +quantity，SELL → -quantity（quantity 本身允许为负，符号自洽）。</li>
 *   <li>开仓/加仓：买入价 × 数量 + 手续费 计入成本。</li>
 *   <li>平仓：按当前持仓均价结算已实现盈亏；卖出手续费冲减已实现。</li>
 *   <li>反向超出当前持仓时自动翻转，剩余部分以本次成交价作为新持仓成本。</li>
 * </ul>
 * 金额内部用高精度计算，均价除法保留 8 位，输出由调用方按 2 位四舍五入。
 */
public final class PositionCalculator {

    private static final int DIV_SCALE = 8;

    private PositionCalculator() {
    }

    /** 计算结果：已实现盈亏、净持仓数量、持仓成本总额（带符号）、持仓均价。 */
    public static final class Result {
        private final BigDecimal realizedProfit;
        private final BigDecimal netQuantity;
        private final BigDecimal costAmount;
        private final BigDecimal avgCost;

        Result(BigDecimal realizedProfit, BigDecimal netQuantity, BigDecimal costAmount, BigDecimal avgCost) {
            this.realizedProfit = realizedProfit;
            this.netQuantity = netQuantity;
            this.costAmount = costAmount;
            this.avgCost = avgCost;
        }

        public BigDecimal getRealizedProfit() {
            return realizedProfit;
        }

        public BigDecimal getNetQuantity() {
            return netQuantity;
        }

        /** 持仓成本总额：多头为正（已付出成本），空头为负（已收到权利金净额）。 */
        public BigDecimal getCostAmount() {
            return costAmount;
        }

        /** 持仓均价（按 |净持仓| 摊销）；无持仓时为 null。 */
        public BigDecimal getAvgCost() {
            return avgCost;
        }
    }

    /**
     * 按时间顺序回放一组属于同一 key 的操作，得出该 key 的持仓与已实现盈亏。
     *
     * @param opsInTimeOrder 已按 tradeDate、createdAt 升序排列的操作
     */
    public static Result compute(List<RealizationOperation> opsInTimeOrder) {
        BigDecimal qty = BigDecimal.ZERO;       // 带符号净持仓
        BigDecimal cost = BigDecimal.ZERO;      // 当前持仓成本基数（>=0 的绝对值口径）
        BigDecimal realized = BigDecimal.ZERO;

        for (RealizationOperation op : opsInTimeOrder) {
            BigDecimal rawQty = nz(op.getQuantity());
            BigDecimal price = nz(op.getPrice());
            BigDecimal fee = nz(op.getFee());
            BigDecimal delta = op.getOperationType() == OperationType.SELL ? rawQty.negate() : rawQty;

            if (delta.signum() == 0) {
                realized = realized.subtract(fee);
                continue;
            }

            boolean opening = qty.signum() == 0 || qty.signum() == delta.signum();
            if (opening) {
                cost = cost.add(price.multiply(delta.abs())).add(fee);
                qty = qty.add(delta);
            } else {
                BigDecimal absQty = qty.abs();
                BigDecimal avgCost = absQty.signum() == 0
                        ? BigDecimal.ZERO
                        : cost.divide(absQty, DIV_SCALE, RoundingMode.HALF_UP);
                BigDecimal closeQty = absQty.min(delta.abs());
                if (qty.signum() > 0) {
                    realized = realized.add(price.subtract(avgCost).multiply(closeQty));
                } else {
                    realized = realized.add(avgCost.subtract(price).multiply(closeQty));
                }
                realized = realized.subtract(fee);

                cost = cost.subtract(avgCost.multiply(closeQty));
                qty = qty.add(delta);
                if (qty.signum() == 0) {
                    cost = BigDecimal.ZERO;
                } else if (delta.abs().compareTo(closeQty) > 0) {
                    // 翻转：剩余部分按本次成交价作为新持仓成本
                    cost = price.multiply(qty.abs());
                }
            }
        }

        BigDecimal avgCost = qty.signum() == 0
                ? null
                : cost.divide(qty.abs(), DIV_SCALE, RoundingMode.HALF_UP);
        BigDecimal signedCost = qty.signum() >= 0 ? cost : cost.negate();
        return new Result(realized, qty, signedCost, avgCost);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
