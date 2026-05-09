package com.funfun.schedule.dto;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易流水 DTO，对应 transaction_flow 表。
 * 注：extra 用 fastjson2 JSONObject，跟生产代码一致；如果走 REST 序列化，
 * Jackson 会把它当 Map 对待，输出标准 JSON。
 */
@Data
public class TransactionFlowDTO {
    private Long id;
    private Long groupId;
    private Long userId;
    private FlowType flowType;
    private TransactionType transactionType;
    private Integer amount;
    private Integer balance;
    private String description;
    private JSONObject extra;
    private Long operator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
