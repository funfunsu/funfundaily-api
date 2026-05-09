package com.funfun.schedule.controller;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.TransactionFlowDTO;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.TransactionType;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.TransactionFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 流水统一入口（前端 apiTs.flow.* 调用此控制器）。
 * 数据底座是 transaction_flow，与生产模型对齐。
 */
@RestController
@RequestMapping("/api/transaction-flows")
public class TransactionFlowController {

    @Autowired
    private TransactionFlowService transactionFlowService;

    @PostMapping("/balance")
    public CommonResponse<Integer> balance(@RequestBody FlowQueryRequest request) {
        FlowType flowType = parseFlowType(request.getFlowType());
        Integer balance = transactionFlowService.getCurrentBalance(
                request.getGroupId(), request.getTargetUserId(), flowType);
        return CommonResponse.success(balance != null ? balance : 0);
    }

    @PostMapping("/list")
    public CommonResponse<List<TransactionFlowDTO>> list(@RequestBody FlowListRequest request) {
        FlowType flowType = parseFlowType(request.getFlowType());
        int pageNo = request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20;
        // 取所有列表后简单分页；用全量 list 是因为前端目前不传时间窗
        List<TransactionFlowDTO> all = transactionFlowService.getTransactionFlowsList(
                request.getGroupId(), request.getTargetUserId(), flowType);
        if (all == null || all.isEmpty()) return CommonResponse.success(Collections.emptyList());
        int from = Math.min((pageNo - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return CommonResponse.success(all.subList(from, to));
    }

    /**
     * 直接保存一条流水（家庭账本 CASH 入口走这里；POINTS 流水通常由打卡/兑换业务自动写）。
     */
    @PostMapping("/save")
    public CommonResponse<TransactionFlowDTO> save(@RequestBody FlowSaveRequest request) {
        if (request == null || request.getFlow() == null) {
            throw new MyException("40004", "flow body is required");
        }
        FlowSaveBody body = request.getFlow();
        FlowType flowType = parseFlowType(body.getFlowType());
        TransactionType txnType = parseTransactionType(body.getTransactionType());

        TransactionFlowDTO dto = new TransactionFlowDTO();
        dto.setFlowType(flowType);
        dto.setTransactionType(txnType);
        dto.setAmount(body.getAmount() == null ? 0 : body.getAmount());
        dto.setDescription(body.getDescription());
        if (body.getExtra() != null) dto.setExtra(new JSONObject(body.getExtra()));

        Long operatorId = UserContext.getUserId();
        TransactionFlowDTO saved = transactionFlowService.saveTransactionFlow(
                dto, request.getGroupId(), request.getTargetUserId(), operatorId);
        return CommonResponse.success(saved);
    }

    private FlowType parseFlowType(String raw) {
        if (raw == null) throw new MyException("40004", "flowType is required");
        try {
            return FlowType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MyException("40004", "unsupported flowType: " + raw);
        }
    }

    private TransactionType parseTransactionType(String raw) {
        if (raw == null) throw new MyException("40004", "transactionType is required");
        try {
            return TransactionType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MyException("40004", "unsupported transactionType: " + raw);
        }
    }

    public static class FlowQueryRequest {
        private String flowType;
        private Long targetUserId;
        private Long groupId;
        public String getFlowType() { return flowType; }
        public void setFlowType(String flowType) { this.flowType = flowType; }
        public Long getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
    }

    public static class FlowListRequest extends FlowQueryRequest {
        private Integer pageNo;
        private Integer pageSize;
        public Integer getPageNo() { return pageNo; }
        public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    public static class FlowSaveBody {
        private String flowType;
        private String transactionType;
        private Integer amount;
        private String description;
        private java.util.Map<String, Object> extra;
        public String getFlowType() { return flowType; }
        public void setFlowType(String flowType) { this.flowType = flowType; }
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public java.util.Map<String, Object> getExtra() { return extra; }
        public void setExtra(java.util.Map<String, Object> extra) { this.extra = extra; }
    }

    public static class FlowSaveRequest {
        private FlowSaveBody flow;
        private Long groupId;
        private Long targetUserId;
        public FlowSaveBody getFlow() { return flow; }
        public void setFlow(FlowSaveBody flow) { this.flow = flow; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public Long getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    }
}
