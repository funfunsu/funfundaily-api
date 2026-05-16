package com.funfun.schedule.dto.point;

/**
 * 兑换结果。
 */
public class PointExchangeResult {

    private Long exchangeRecordId;
    private Integer balanceAfter;

    public PointExchangeResult() {
    }

    public PointExchangeResult(Long exchangeRecordId, Integer balanceAfter) {
        this.exchangeRecordId = exchangeRecordId;
        this.balanceAfter = balanceAfter;
    }

    public Long getExchangeRecordId() {
        return exchangeRecordId;
    }

    public void setExchangeRecordId(Long exchangeRecordId) {
        this.exchangeRecordId = exchangeRecordId;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
}
