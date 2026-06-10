package com.funfun.schedule.service.support;

import com.funfun.schedule.dto.BatchStatsDTO;
import com.funfun.schedule.dto.OptionKeyStatsDTO;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.entity.RealizationOperation;
import com.funfun.schedule.enums.InstrumentType;
import com.funfun.schedule.enums.OperationType;
import com.funfun.schedule.enums.OptionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link BatchStatsCalculator} 批次卡片汇总单元测试（正股 + 多个期权 key）。
 */
class BatchStatsCalculatorTest {

    private static final LocalDate EXP = LocalDate.of(2026, 6, 20);
    private int seq = 0;

    private RealizationOperation stock(OperationType type, String price, String qty) {
        return op(InstrumentType.STOCK, type, price, qty, null, null);
    }

    private RealizationOperation option(OperationType type, String price, String qty,
                                        OptionType ot, String strike) {
        return op(InstrumentType.OPTION, type, price, qty, ot, strike);
    }

    private RealizationOperation op(InstrumentType inst, OperationType type, String price, String qty,
                                    OptionType ot, String strike) {
        RealizationOperation o = new RealizationOperation();
        o.setInstrument(inst);
        o.setOperationType(type);
        o.setPrice(new BigDecimal(price));
        o.setQuantity(new BigDecimal(qty));
        o.setFee(BigDecimal.ZERO);
        o.setTradeDate(LocalDate.of(2026, 1, 1).plusDays(seq++));
        if (inst == InstrumentType.OPTION) {
            o.setOptionType(ot);
            o.setStrikePrice(new BigDecimal(strike));
            o.setExpirationDate(EXP);
        }
        return o;
    }

    private void assertBd(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void mixedStockAndOptionKeys() {
        RealizationBatch batch = new RealizationBatch();
        batch.setBatchId(1L);
        batch.setPlanBuyPrice(new BigDecimal("10"));
        batch.setPlanSellPrice(new BigDecimal("15"));
        batch.setQuantity(new BigDecimal("100"));

        List<RealizationOperation> ops = new ArrayList<>();
        ops.add(stock(OperationType.BUY, "10", "100"));
        ops.add(stock(OperationType.SELL, "14", "50"));               // realized (14-10)*50 = 200
        ops.add(option(OperationType.BUY, "1", "2", OptionType.CALL, "12"));
        ops.add(option(OperationType.SELL, "3", "1", OptionType.CALL, "12")); // realized (3-1)*1 = 2
        ops.add(option(OperationType.SELL, "2", "1", OptionType.PUT, "8"));   // sell-to-open short put

        BatchStatsDTO dto = BatchStatsCalculator.computeStats(batch, ops);

        assertBd("500", dto.getTargetProfit());           // (15-10)*100
        assertBd("200", dto.getStockRealizedProfit());
        assertBd("50", dto.getStockQuantity());
        assertBd("10", dto.getStockCostPrice());
        assertBd("500", dto.getStockCostAmount());
        assertBd("2", dto.getOptionRealizedProfit());
        assertBd("202", dto.getTotalRealizedProfit());

        assertEquals(2, dto.getOptionKeys().size());
        OptionKeyStatsDTO call = dto.getOptionKeys().stream()
                .filter(k -> k.getOptionType() == OptionType.CALL).findFirst().orElseThrow();
        assertBd("1", call.getNetQuantity());
        assertBd("2", call.getRealizedProfit());
        assertBd("1", call.getCostAmount());

        OptionKeyStatsDTO put = dto.getOptionKeys().stream()
                .filter(k -> k.getOptionType() == OptionType.PUT).findFirst().orElseThrow();
        assertBd("-1", put.getNetQuantity());
        assertBd("0", put.getRealizedProfit());
        assertBd("-2", put.getCostAmount());
    }
}
