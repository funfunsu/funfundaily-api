package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.TransactionFlowDTO;
import com.funfun.schedule.dto.point.PointExchangeCommand;
import com.funfun.schedule.dto.point.PointExchangeQuery;
import com.funfun.schedule.dto.point.PointExchangeRecordItem;
import com.funfun.schedule.dto.point.PointExchangeRecordPageResult;
import com.funfun.schedule.dto.point.PointExchangeResult;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.PointProduct;
import com.funfun.schedule.entity.TransactionFlow;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.PointProductStatus;
import com.funfun.schedule.enums.TransactionType;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.repository.PointProductRepository;
import com.funfun.schedule.repository.TransactionFlowRepository;
import com.funfun.schedule.service.PointExchangeService;
import com.funfun.schedule.service.TransactionFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分兑换领域服务实现。
 *
 * 设计要点：
 *  - 兑换数据**只**写 transaction_flow 一张表（FlowType=POINTS, TransactionType=EXPENSE）。
 *  - 兑换记录的"商品名/商品 id"放在流水的 extra JSON 里（productName / productId）。
 *  - 余额单一真源：transaction_flow 最近一条 balance；group_member.score 仅同步兜底。
 *  - 已不再使用 point_exchange_record 表。
 */
@Service
public class PointExchangeServiceImpl implements PointExchangeService {

    private static final String EXTRA_KEY_PRODUCT_ID = "productId";
    private static final String EXTRA_KEY_PRODUCT_NAME = "productName";

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private PointProductRepository pointProductRepository;

    @Autowired
    private TransactionFlowService transactionFlowService;

    @Autowired
    private TransactionFlowRepository transactionFlowRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointExchangeResult exchange(PointExchangeCommand command) {
        validateExchangeCommand(command);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserIdForUpdate(command.getGroupId(), command.getUserId())
                .orElseThrow(() -> new MyException("40004", "cross-group operation forbidden"));

        PointProduct product = pointProductRepository.findByIdForUpdate(command.getProductId())
                .orElseThrow(() -> new MyException("40401", "product not found"));

        validateProductForExchange(command, member, product);

        // 读真实余额，含打卡入账 + 历次兑换扣减
        Integer currentBalance = transactionFlowService.getCurrentBalance(
                command.getGroupId(), command.getUserId(), FlowType.POINTS);
        if (currentBalance == null) currentBalance = 0;
        int newBalance = currentBalance - product.getRequiredScore();
        if (newBalance < 0) {
            throw new MyException("40001", "insufficient score");
        }

        // 同步刷新 group_member.score 给老代码兜底
        member.setScore(newBalance);
        groupMemberRepository.save(member);

        // 写 EXPENSE 流水：amount 为负，balance 由 service 内部 (currentBalance + amount) 叠加，正好 = newBalance。
        // 商品信息放 extra；description 走人类可读文本。
        TransactionFlowDTO flow = new TransactionFlowDTO();
        flow.setFlowType(FlowType.POINTS);
        flow.setTransactionType(TransactionType.EXPENSE);
        flow.setAmount(-product.getRequiredScore());
        flow.setDescription("兑换：" + product.getName());
        JSONObject extra = new JSONObject();
        extra.put(EXTRA_KEY_PRODUCT_ID, product.getId());
        extra.put(EXTRA_KEY_PRODUCT_NAME, product.getName());
        flow.setExtra(extra);
        TransactionFlowDTO saved = transactionFlowService.saveTransactionFlow(
                flow, command.getGroupId(), command.getUserId(), command.getOperator());

        return new PointExchangeResult(saved.getId(), newBalance);
    }

    @Override
    public PointExchangeRecordPageResult queryRecords(PointExchangeQuery query) {
        validateRecordQuery(query);

        GroupMember requester = groupMemberRepository.findByGroupIdAndUserId(query.getGroupId(), query.getRequesterUserId())
                .orElseThrow(() -> new MyException("40301", "permission denied"));

        boolean isManager = isManagerRole(requester.getRole());
        Long targetUserId = query.getUserId();
        if (targetUserId == null) targetUserId = query.getRequesterUserId();

        if (!isManager && !targetUserId.equals(query.getRequesterUserId())) {
            throw new MyException("40301", "permission denied");
        }

        int pageNo = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : Math.min(query.getPageSize(), 100);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        LocalDate startDate = query.getStartDate();
        LocalDate endDate = query.getEndDate();
        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        // 用 endDate+1 天的 0 点作为右开区间
        LocalDateTime endDateTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new MyException("42201", "invalid request params");
        }

        // 家长不指定 userId 时拉群组所有成员的兑换流水
        Long queryUserId = (isManager && query.getUserId() == null) ? null : targetUserId;

        Page<TransactionFlow> flowPage = transactionFlowRepository.queryFlowsByType(
                query.getGroupId(), queryUserId, FlowType.POINTS, TransactionType.EXPENSE,
                startDateTime, endDateTime, pageable);

        List<PointExchangeRecordItem> items = flowPage.getContent().stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        PointExchangeRecordPageResult result = new PointExchangeRecordPageResult();
        result.setTotal(flowPage.getTotalElements());
        result.setList(items);
        return result;
    }

    /**
     * TransactionFlow → 兑换记录视图。商品信息从 extra JSON 解析。
     */
    private PointExchangeRecordItem toItem(TransactionFlow flow) {
        PointExchangeRecordItem item = new PointExchangeRecordItem();
        item.setId(flow.getId());
        item.setUserId(flow.getUserId());
        item.setGroupId(flow.getGroupId());
        item.setBalanceAfter(flow.getBalance());
        // amount 是负数，scoreDeducted 取绝对值方便前端展示
        item.setScoreDeducted(flow.getAmount() == null ? 0 : Math.abs(flow.getAmount()));
        item.setExchangeTime(flow.getCreatedAt());

        if (flow.getExtra() != null && !flow.getExtra().isEmpty()) {
            try {
                JSONObject extra = JSON.parseObject(flow.getExtra());
                item.setProductId(extra.getLong(EXTRA_KEY_PRODUCT_ID));
                item.setProductName(extra.getString(EXTRA_KEY_PRODUCT_NAME));
            } catch (Exception ignore) {
                // extra 解析失败不阻塞展示
            }
        }
        // 兜底：extra 没有 productName 就退回 description（"兑换：xxx"格式）
        if (item.getProductName() == null && flow.getDescription() != null) {
            String desc = flow.getDescription();
            int idx = desc.indexOf('：');
            if (idx < 0) idx = desc.indexOf(':');
            item.setProductName(idx >= 0 ? desc.substring(idx + 1) : desc);
        }
        return item;
    }

    private void validateExchangeCommand(PointExchangeCommand command) {
        if (command == null) {
            throw new MyException("42201", "invalid request params");
        }
        validateId(command.getGroupId());
        validateId(command.getUserId());
        validateId(command.getProductId());
        validateId(command.getOperator());
        if (command.getProductRequiredScore() == null || command.getProductRequiredScore() <= 0) {
            throw new MyException("42201", "invalid request params");
        }
    }

    private void validateRecordQuery(PointExchangeQuery query) {
        if (query == null) {
            throw new MyException("42201", "invalid request params");
        }
        validateId(query.getGroupId());
        validateId(query.getRequesterUserId());
        if (query.getUserId() != null && query.getUserId() <= 0) {
            throw new MyException("42201", "invalid request params");
        }
    }

    private void validateProductForExchange(PointExchangeCommand command, GroupMember member, PointProduct product) {
        if (!product.getGroupId().equals(command.getGroupId()) || !member.getGroupId().equals(command.getGroupId())) {
            throw new MyException("40004", "cross-group operation forbidden");
        }
        if (PointProductStatus.REMOVED.name().equalsIgnoreCase(product.getStatus())) {
            throw new MyException("40002", "product unavailable");
        }
        if (!product.getRequiredScore().equals(command.getProductRequiredScore())) {
            throw new MyException("40003", "product info changed");
        }
    }

    private void validateId(Long value) {
        if (value == null || value <= 0) {
            throw new MyException("42201", "invalid request params");
        }
    }

    /** 角色识别：兼容 Creator/Admin 与 OWNER/ADMIN 语义。 */
    private boolean isManagerRole(String role) {
        if (role == null) return false;
        return "ADMIN".equalsIgnoreCase(role)
                || "OWNER".equalsIgnoreCase(role)
                || "CREATOR".equalsIgnoreCase(role);
    }
}
