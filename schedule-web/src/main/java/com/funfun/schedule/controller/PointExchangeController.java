package com.funfun.schedule.controller;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.point.PointExchangeCommand;
import com.funfun.schedule.dto.point.PointExchangeQuery;
import com.funfun.schedule.dto.point.PointExchangeRecordDTO;
import com.funfun.schedule.dto.point.PointExchangeRecordItem;
import com.funfun.schedule.dto.point.PointExchangeRecordPageResult;
import com.funfun.schedule.dto.point.PointExchangeResult;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.PointExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分兑换控制器，提供兑换执行与记录查询的RESTful API接口。
 * 关联需求：Req-3（兑换资格校验）、Req-4（兑换记录追踪）、Req-5（数据边界）
 */
@RestController
@RequestMapping("/api/point/exchange")
public class PointExchangeController {

    @Autowired
    private PointExchangeService pointExchangeService;

    /**
     * API-5: 发起积分兑换
     * 关联需求: Req-3, Req-4, Req-5
     * 权限: 群组内所有成员可兑换
     * 
     * 业务逻辑:
     * 1. 校验积分余额是否充足
     * 2. 校验商品是否可用（非REMOVED状态）
     * 3. 校验商品信息（requiredScore）是否与DB最新一致
     * 4. 在数据库事务内扣减积分、创建兑换记录、创建积分流水
     * 5. 并发场景下通过SELECT FOR UPDATE保证一致性
     *
     * @param request 兑换请求
     * @return 兑换结果，包含记录ID和扣减后余额
     */
    @PostMapping("")
    public CommonResponse<ExchangeResponse> exchange(@RequestBody ExchangeRequest request) {
        PointExchangeCommand command = new PointExchangeCommand();
        command.setGroupId(request.getGroupId());
        command.setUserId(UserContext.getUserId());
        command.setProductId(request.getProductId());
        command.setProductRequiredScore(request.getProductRequiredScore());
        command.setOperator(UserContext.getUserId());

        PointExchangeResult result = pointExchangeService.exchange(command);
        return CommonResponse.success(new ExchangeResponse(result.getExchangeRecordId(), result.getBalanceAfter()));
    }

    /**
     * API-6: 查询兑换记录
     * 关联需求: Req-4
     * 权限:
     * - 家长(OWNER/ADMIN)可查询群组内任意成员的兑换记录
     * - 普通成员仅可查询自己的兑换记录
     *
     * @param groupId   群组ID，必须
     * @param userId    目标用户ID，可选。如果为空则查询当前用户
     * @param startDate 开始日期，可选，格式yyyy-MM-dd
     * @param endDate   结束日期，可选，格式yyyy-MM-dd
     * @param page      页码，默认为1
     * @param pageSize  每页数量，默认为20
     * @return 分页的兑换记录列表
     */
    @GetMapping("/records")
    public CommonResponse<PointExchangeRecordsResponse> queryRecords(
            @RequestParam Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        Long currentUserId = UserContext.getUserId();
        Long targetUserId = userId != null ? userId : currentUserId;

        PointExchangeQuery query = new PointExchangeQuery();
        query.setGroupId(groupId);
        query.setRequesterUserId(currentUserId);
        query.setUserId(targetUserId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setPage(page);
        query.setPageSize(pageSize);

        PointExchangeRecordPageResult pageResult = pointExchangeService.queryRecords(query);
        
        List<PointExchangeRecordDTO> recordDTOs = pageResult.getList().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PointExchangeRecordsResponse response = new PointExchangeRecordsResponse(
                pageResult.getTotal(),
                recordDTOs
        );
        return CommonResponse.success(response);
    }

    /**
     * 兑换记录视图（来自 transaction_flow）→ API 响应 DTO。
     */
    private PointExchangeRecordDTO convertToDTO(PointExchangeRecordItem item) {
        return new PointExchangeRecordDTO(
                item.getId(),
                item.getUserId(),
                item.getGroupId(),
                item.getProductId(),
                item.getProductName(),
                item.getScoreDeducted(),
                item.getBalanceAfter(),
                item.getExchangeTime()
        );
    }

    /**
     * 兑换请求体
     */
    public static class ExchangeRequest {
        private Long groupId;
        private Long productId;
        private Integer productRequiredScore;

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

        public Integer getProductRequiredScore() {
            return productRequiredScore;
        }

        public void setProductRequiredScore(Integer productRequiredScore) {
            this.productRequiredScore = productRequiredScore;
        }
    }

    /**
     * 兑换响应体
     */
    public static class ExchangeResponse {
        private Long exchangeRecordId;
        private Integer balanceAfter;

        public ExchangeResponse(Long exchangeRecordId, Integer balanceAfter) {
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

    /**
     * 兑换记录分页响应体
     */
    public static class PointExchangeRecordsResponse {
        private Long total;
        private List<PointExchangeRecordDTO> list;

        public PointExchangeRecordsResponse(Long total, List<PointExchangeRecordDTO> list) {
            this.total = total;
            this.list = list;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public List<PointExchangeRecordDTO> getList() {
            return list;
        }

        public void setList(List<PointExchangeRecordDTO> list) {
            this.list = list;
        }
    }
}
