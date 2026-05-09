package com.funfun.schedule.dto.point;

/**
 * 发起积分兑换命令。
 */
public class PointExchangeCommand {

    private Long groupId;
    private Long userId;
    private Long productId;
    private Integer productRequiredScore;
    private Long operator;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getProductRequiredScore() {
        return productRequiredScore;
    }

    public void setProductRequiredScore(Integer productRequiredScore) {
        this.productRequiredScore = productRequiredScore;
    }

    public Long getOperator() {
        return operator;
    }

    public void setOperator(Long operator) {
        this.operator = operator;
    }
}
