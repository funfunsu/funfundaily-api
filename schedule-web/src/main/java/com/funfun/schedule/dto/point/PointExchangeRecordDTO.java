package com.funfun.schedule.dto.point;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 兑换记录DTO，用于API响应。
 */
public class PointExchangeRecordDTO {

    private Long id;
    private Long userId;
    private Long groupId;
    private Long productId;
    private String productName;
    private Integer scoreDeducted;
    private Integer balanceAfter;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime exchangeTime;

    public PointExchangeRecordDTO() {
    }

    public PointExchangeRecordDTO(Long id, Long userId, Long groupId, Long productId, 
                                   String productName, Integer scoreDeducted, 
                                   Integer balanceAfter, LocalDateTime exchangeTime) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.productId = productId;
        this.productName = productName;
        this.scoreDeducted = scoreDeducted;
        this.balanceAfter = balanceAfter;
        this.exchangeTime = exchangeTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getScoreDeducted() {
        return scoreDeducted;
    }

    public void setScoreDeducted(Integer scoreDeducted) {
        this.scoreDeducted = scoreDeducted;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getExchangeTime() {
        return exchangeTime;
    }

    public void setExchangeTime(LocalDateTime exchangeTime) {
        this.exchangeTime = exchangeTime;
    }
}
