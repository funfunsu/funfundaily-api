package com.funfun.schedule.service.impl;

import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.entity.ScoreFlow;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.repository.ScoreFlowRepository;
import com.funfun.schedule.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ScoreService接口的实现类，实现积分相关的业务逻辑
 */
@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private ScoreFlowRepository scoreFlowRepository;

    @Autowired
    private CheckinRecordRepository checkinRecordRepository;

    @Override
    @Transactional
    public ScoreFlow distributeScoreForTask(CheckinRecord checkinRecord, Integer score, Long operator, Map<String, Object> extInfo) {

        ScoreFlow scoreFlow = new ScoreFlow();
        return scoreFlowRepository.save(scoreFlow);
    }

    @Override
    @Transactional
    public ScoreFlow redeemScore(Long userId, Long groupId, Integer score, String eventName, Long operator, Map<String, Object> extInfo) {

        ScoreFlow scoreFlow = new ScoreFlow();
        return scoreFlowRepository.save(scoreFlow);
    }

    @Override
    public Integer getUserScoreBalance(Long userId, Long groupId) {
        // 查询用户在指定群组的积分流水，计算余额
        List<ScoreFlow> scoreFlows = scoreFlowRepository.findByGroupIdAndUserId(groupId, userId);
        return scoreFlows.stream()
                .filter(flow -> !flow.getDeleted())
                .mapToInt(ScoreFlow::getScore)
                .sum();
    }

    @Override
    public List<ScoreFlow> getUserScoreFlows(Long userId, Long groupId, Date startDate, Date endDate) {
        return List.of();
    }

    @Override
    public ScoreFlow getScoreFlowById(Long id) {
        Optional<ScoreFlow> optionalScoreFlow = scoreFlowRepository.findById(id);
        return optionalScoreFlow.orElseThrow(() -> new RuntimeException("积分流水记录不存在"));
    }

    @Override
    public List<ScoreFlow> getScoreFlowsByIds(List<Long> ids) {
        return scoreFlowRepository.findByIdIn(ids);
    }

    @Override
    public boolean checkScoreSufficient(Long userId, Long groupId, Integer requiredScore) {
        Integer balance = getUserScoreBalance(userId, groupId);
        return balance >= requiredScore;
    }
}