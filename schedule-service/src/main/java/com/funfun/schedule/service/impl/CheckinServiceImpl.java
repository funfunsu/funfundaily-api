package com.funfun.schedule.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper; // For handling extra JSON data if needed
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.entity.ScoreFlow;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.mapper.CheckinRecordMapper;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.repository.ScoreFlowRepository;
import com.funfun.schedule.service.CheckinService;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CheckinServiceImpl implements CheckinService {

    private static final Logger logger = LoggerFactory.getLogger(CheckinServiceImpl.class);

    @Autowired
    private CheckinRecordRepository checkinRecordRepository;

    @Autowired
    private ScoreFlowRepository scoreFlowRepository;

    @Autowired
    private CheckinRecordMapper checkinRecordMapper;

    @Autowired
    private ObjectMapper objectMapper; // 如果需要处理 extra 字段为 JSON

    @Override
    @Transactional // 确保打卡和积分记录在同一事务中
    public Long performCheckin(CheckinRecordDTO requestDto) {

        Long userId = requestDto.getUserId(); // 从安全上下文获取更佳
        Long taskId = requestDto.getTaskId();
        Long groupId = requestDto.getGroupId();
        Long operatorId = requestDto.getOperatorId(); // 操作人，通常是用户自己

        // --- 1. 前置校验 (简化版) ---
        // 在实际应用中，你应该在这里调用 TaskService, UserService, GroupService 来确认
        // - Task 是否存在且有效
        // - User 是否存在
        // - Group 是否存在
        // - User 是否属于该 Group
        // - Task 是否属于该 Group
        // - 当前时间是否在任务允许打卡的时间范围内
        // - 用户今天是否已经打过此卡 (去重校验)

        // 示例：简单的重复打卡检查
        Optional<CheckinRecord> existingRecord = checkinRecordRepository.findByUserIdAndTaskIdAndGroupId(userId, taskId, groupId);
        if (existingRecord.isPresent()) {
            // 这里可以根据业务需求决定是抛出异常还是忽略
            logger.warn("User {} has already checked in for task {} in group {}", userId, taskId, groupId);
            CommonException.DATA_DUPLICATE.throwsError("已经为任务打过卡了");
        }

        // --- 2. 创建打卡记录 ---
        CheckinRecord checkinRecord = checkinRecordMapper.toEntity(requestDto);
        // 如果需要记录额外信息，可以设置 extra 字段
        // checkinRecord.setExtra(requestDto.getExtraInfo()); // 假设 requestDto 有这个字段

        CheckinRecord savedRecord = checkinRecordRepository.save(checkinRecord);
        logger.info("Checkin record created with ID: {}", savedRecord.getId());

        // --- 3. 计算积分 (示例) ---
        // Integer earnedScore = scoreCalculator.calculateForCheckin(taskId, userId); // 假设有这样的方法
        Integer earnedScore = 10; // 示例固定积分
        String eventName = "TASK_CHECKIN";
        String label = "完成任务打卡";

        // --- 4. 获取用户当前积分余额 (关键步骤!) ---
        // 这里简化处理，假设有一个方法或表来获取最新余额
        // Integer currentBalance = getUserCurrentScore(userId, groupId); // 你需要实现这个逻辑
        Integer currentBalance = 100; // 示例初始余额
        Integer newBalance = currentBalance + earnedScore; // 计算新的余额

        // --- 5. 创建积分流水记录 ---
        ScoreFlow scoreFlow = new ScoreFlow(
                0, // flow_type: 0 - 入账
                earnedScore,
                newBalance, // remain_score: 记录发生后的余额
                userId,
                groupId,
                eventName,
                label,
                operatorId // 操作人
        );
        // 如果需要记录额外信息，可以设置 extra 字段
        try {
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("checkinRecordId", savedRecord.getId());
            // extraData.put("otherInfo", requestDto.getSomeOtherInfo());
            scoreFlow.setExtra(objectMapper.writeValueAsString(extraData));
        } catch (Exception e) {
            logger.error("Failed to serialize extra data for score flow", e);
            // 根据策略决定是否继续或回滚
        }

        scoreFlowRepository.save(scoreFlow);
        logger.info("Score flow record created with ID: {} for checkin ID: {}", scoreFlow.getId(), savedRecord.getId());

        // --- 6. 更新用户总积分 (如果积分余额存储在单独的表中) ---
        // updateUserTotalScore(userId, groupId, newBalance); // 你需要实现这个逻辑

        return savedRecord.getId();


    }

    @Override
    public List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, LocalDateTime from, LocalDateTime to) {
        // --- 参数校验 (可选但推荐) ---
        if (groupId == null || userId == null || from == null || to == null) {
            logger.warn("Invalid parameters for getRecordList: groupId={}, userId={}, from={}, to={}", groupId, userId, from, to);
            // 可以抛出 IllegalArgumentException 或返回空列表
            return List.of(); // 返回空列表
            // throw new IllegalArgumentException("Parameters cannot be null");
        }


        // --- 调用 Repository 查询 ---
        List<CheckinRecord> records = checkinRecordRepository.findByGroupIdAndUserIdAndCompleteTimeBetween(groupId, userId, from, to);
        return checkinRecordMapper.toDTOList(records);
    }
}