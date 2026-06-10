package com.funfun.schedule.service.support;

import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OperationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link PositionCalculator} 带符号加权平均成本计算单元测试。
 */
class PositionCalculatorTest {

    private RealizationOperation op(OperationType type, String price, String qty, String fee, int seq) {
        RealizationOperation o = new RealizationOperation();
        o.setInstrument(InstrumentType.STOCK);
        o.setOperationType(type);
        o.setPrice(new BigDecimal(price));
        o.setQuantity(new BigDecimal(qty));
        o.setFee(new BigDecimal(fee));
        o.setTradeDate(LocalDate.of(2026, 1, 1).plusDays(seq));
        return o;
    }

    private void assertBd(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void longRoundTrip_realizesWeightedAverage() {
        List<RealizationOperation> ops = new ArrayList<>();
        ops.add(op(OperationType.BUY, "100", "10", "0", 0));
        ops.add(op(OperationType.SELL, "120", "4", "0", 1));
        ops.add(op(OperationType.SELL, "90", "6", "0", 2));

        PositionCalculator.Result r = PositionCalculator.compute(ops);
        // (120-100)*4 + (90-100)*6 = 80 - 60 = 20
        assertBd("20", r.getRealizedProfit());
        assertBd("0", r.getNetQuantity());
        assertNull(r.getAvgCost());
    }

    @Test
    void buyFeeFoldsIntoCost_sellFeeReducesRealized() {
        List<RealizationOperation> ops = new ArrayList<>();
        ops.add(op(OperationType.BUY, "100", "10", "5", 0)); // avgCost = 1005/10 = 100.5
        ops.add(op(OperationType.SELL, "110", "10", "5", 1));

        PositionCalculator.Result r = PositionCalculator.compute(ops);
        // (110 - 100.5)*10 - 5 = 95 - 5 = 90
        assertBd("90", r.getRealizedProfit());
        assertBd("0", r.getNetQuantity());
    }

    @Test
    void shortThenCover_realizesPremiumMinusBuyback() {
        List<RealizationOperation> ops = new ArrayList<>();
        ops.add(op(OperationType.SELL, "50", "5", "0", 0)); // sell to open -> short 5 @ 50
        ops.add(op(OperationType.BUY, "40", "5", "0", 1));  // buy to close

        PositionCalculator.Result r = PositionCalculator.compute(ops);
        // short: (50 - 40)*5 = 50
        assertBd("50", r.getRealizedProfit());
        assertBd("0", r.getNetQuantity());
    }

    @Test
    void flipThroughZero_keepsRemainderAtNewPrice() {
        List<RealizationOperation> ops = new ArrayList<>();
        ops.add(op(OperationType.BUY, "100", "5", "0", 0));
        ops.add(op(OperationType.SELL, "110", "8", "0", 1)); // close 5 long, open 3 short @110

        PositionCalculator.Result r = PositionCalculator.compute(ops);
        assertBd("50", r.getRealizedProfit());        // (110-100)*5
        assertBd("-3", r.getNetQuantity());            // net short 3
        assertBd("110", r.getAvgCost());               // new short basis
        assertBd("-330", r.getCostAmount());           // signed cost for short
    }

    @Test
    void openLongOnly_reportsCostAndAvg() {
        List<RealizationOperation> ops = new ArrayList<>();
        ops.add(op(OperationType.BUY, "20", "100", "0", 0));
        ops.add(op(OperationType.BUY, "30", "100", "0", 1)); // avg = (2000+3000)/200 = 25

        PositionCalculator.Result r = PositionCalculator.compute(ops);
        assertBd("0", r.getRealizedProfit());
        assertBd("200", r.getNetQuantity());
        assertBd("25", r.getAvgCost());
        assertBd("5000", r.getCostAmount());
    }
}
