package com.funfun.schedule.service;

import com.alibaba.fastjson2.JSON;
import com.funfun.schedule.dto.TransactionFlowDTO;
import com.funfun.schedule.entity.TransactionFlow;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.mapper.TransactionFlowMapper;
import com.funfun.schedule.repository.TransactionFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 交易流水统一入口（替代原有 ScoreService/ScoreFlow 的全部能力）。
 * 写：每次写入会先查当前 balance，叠加 amount 形成新 balance；用 @Transactional 保证一致。
 * 读：余额=最近一条流水的 balance。
 */
@Service
@Transactional
public class TransactionFlowService {

    @Autowired
    private TransactionFlowRepository transactionFlowRepository;

    @Autowired
    private TransactionFlowMapper transactionFlowMapper;

    public TransactionFlowDTO saveTransactionFlow(TransactionFlowDTO flowDTO, Long groupId, Long userId, Long operatorId) {
        TransactionFlow entity = new TransactionFlow();
        entity.setGroupId(groupId);
        entity.setUserId(userId);
        entity.setFlowType(flowDTO.getFlowType());
        entity.setTransactionType(flowDTO.getTransactionType());
        entity.setOperator(operatorId);
        entity.setAmount(flowDTO.getAmount());
        entity.setDescription(flowDTO.getDescription());
        entity.setExtra(flowDTO.getExtra() == null ? null : JSON.toJSONString(flowDTO.getExtra()));

        Integer currentBalance = getCurrentBalance(groupId, userId, flowDTO.getFlowType());
        if (currentBalance == null) currentBalance = 0;
        entity.setBalance(currentBalance + (flowDTO.getAmount() == null ? 0 : flowDTO.getAmount()));
        TransactionFlow saved = transactionFlowRepository.save(entity);
        return transactionFlowMapper.toDTO(saved);
    }

    public Optional<TransactionFlowDTO> getTransactionFlowById(Long id) {
        return transactionFlowRepository.findById(id).map(transactionFlowMapper::toDTO);
    }

    public void deleteTransactionFlow(Long id) {
        transactionFlowRepository.deleteById(id);
    }

    public Page<TransactionFlowDTO> getTransactionFlows(Long groupId, Long userId, FlowType flowType,
                                                       LocalDateTime startTime, LocalDateTime endTime,
                                                       int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNo - 1), pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (startTime != null && endTime != null) {
            return transactionFlowRepository
                    .findByGroupIdAndUserIdAndFlowTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                            groupId, userId, flowType, startTime, endTime, pageable)
                    .map(transactionFlowMapper::toDTO);
        }
        return transactionFlowRepository
                .findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(groupId, userId, flowType, pageable)
                .map(transactionFlowMapper::toDTO);
    }

    public List<TransactionFlowDTO> getTransactionFlowsList(Long groupId, Long userId, FlowType flowType) {
        List<TransactionFlow> entities = transactionFlowRepository
                .findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(groupId, userId, flowType);
        return entities.stream().map(transactionFlowMapper::toDTO).collect(Collectors.toList());
    }

    public Integer getCurrentBalance(Long groupId, Long userId, FlowType flowType) {
        Optional<TransactionFlow> opt = transactionFlowRepository
                .findFirstByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(groupId, userId, flowType);
        return opt.map(TransactionFlow::getBalance).orElse(0);
    }

    public Integer getTotalPoints(Long groupId, Long userId) {
        return getCurrentBalance(groupId, userId, FlowType.POINTS);
    }

    public Integer getTotalCash(Long groupId, Long userId) {
        return getCurrentBalance(groupId, userId, FlowType.CASH);
    }
}
