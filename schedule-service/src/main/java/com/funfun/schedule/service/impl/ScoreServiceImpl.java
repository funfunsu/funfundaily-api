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
        // 验证参数
        if (checkinRecord == null || checkinRecord.getTaskId() == null || checkinRecord.getUserId() == null || checkinRecord.getGroupId() == null) {
            throw new RuntimeException("任务ID、用户ID和群组ID不能为空");
        }
        if (score == null || score <= 0) {
            throw new RuntimeException("积分数量必须大于0");
        }
        if (operator == null) {
            throw new RuntimeException("操作人ID不能为空");
        }

        // 检查是否已为该任务记录发放过积分（防止重复发放）
        Optional<CheckinRecord> existingRecord = checkinRecordRepository.findByTaskIdAndUserId(checkinRecord.getTaskId(), checkinRecord.getUserId());
        if (existingRecord.isPresent()) {
            throw new RuntimeException("该任务已完成并发放积分");
        }

        // 保存签到记录
        checkinRecord.setCompleteStatus(1); // 设置为已完成
        checkinRecord.setCompleteTime(new Date());
        checkinRecord.setDeleteFlag(0);
        checkinRecordRepository.save(checkinRecord);

        // 创建积分流水记录
        ScoreFlow scoreFlow = new ScoreFlow();
        scoreFlow.setScore(score);
        scoreFlow.setUserId(checkinRecord.getUserId());
        scoreFlow.setGroupId(checkinRecord.getGroupId());
        scoreFlow.setEventName("task_completion");
        scoreFlow.setCreateTime(new Date());
        scoreFlow.setExtInfo(extInfo);
        scoreFlow.setOperator(operator);
        scoreFlow.setDeleteFlag(0);

        return scoreFlowRepository.save(scoreFlow);
    }

    @Override
    @Transactional
    public ScoreFlow redeemScore(Long userId, Long groupId, Integer score, String eventName, Long operator, Map<String, Object> extInfo) {
        // 验证参数
        if (userId == null || groupId == null) {
            throw new RuntimeException("用户ID和群组ID不能为空");
        }
        if (score == null || score <= 0) {
            throw new RuntimeException("积分数量必须大于0");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new RuntimeException("事件名称不能为空");
        }
        if (operator == null) {
            throw new RuntimeException("操作人ID不能为空");
        }

        // 检查用户积分是否足够
        Integer balance = getUserScoreBalance(userId, groupId);
        if (balance < score) {
            throw new RuntimeException("积分余额不足，当前余额：" + balance);
        }

        // 创建积分流水记录（负数表示减少）
        ScoreFlow scoreFlow = new ScoreFlow();
        scoreFlow.setScore(-score);
        scoreFlow.setUserId(userId);
        scoreFlow.setGroupId(groupId);
        scoreFlow.setEventName(eventName);
        scoreFlow.setCreateTime(new Date());
        scoreFlow.setExtInfo(extInfo);
        scoreFlow.setOperator(operator);
        scoreFlow.setDeleteFlag(0);

        return scoreFlowRepository.save(scoreFlow);
    }

    @Override
    public Integer getUserScoreBalance(Long userId, Long groupId) {
        // 查询用户在指定群组的积分流水，计算余额
        List<ScoreFlow> scoreFlows = scoreFlowRepository.findByGroupIdAndUserId(groupId, userId);
        return scoreFlows.stream()
                .filter(flow -> flow.getDeleteFlag() == 0)
                .mapToInt(ScoreFlow::getScore)
                .sum();
    }

    @Override
    public List<ScoreFlow> getUserScoreFlows(Long userId, Long groupId, Date startDate, Date endDate) {
        // 查询用户在指定群组、指定时间范围内的积分流水
        List<ScoreFlow> scoreFlows = scoreFlowRepository.findByGroupIdAndUserId(groupId, userId);
        if (startDate != null && endDate != null) {
            return scoreFlows.stream()
                    .filter(flow -> flow.getDeleteFlag() == 0)
                    .filter(flow -> !flow.getCreateTime().before(startDate) && !flow.getCreateTime().after(endDate))
                    .collect(Collectors.toList());
        } else {
            return scoreFlows.stream()
                    .filter(flow -> flow.getDeleteFlag() == 0).collect(Collectors.toList());
        }
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