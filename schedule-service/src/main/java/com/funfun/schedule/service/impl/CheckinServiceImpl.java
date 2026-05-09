package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.dto.TransactionFlowDTO;
import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.TransactionType;
import com.funfun.schedule.mapper.CheckinRecordMapper;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.service.CheckinService;
import com.funfun.schedule.service.ScheduleItemService;
import com.funfun.schedule.service.TransactionFlowService;
import com.funfun.schedule.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CheckinServiceImpl implements CheckinService {

    private static final Logger logger = LoggerFactory.getLogger(CheckinServiceImpl.class);

    private static final String SCORE_KEY = "score";
    private static final String TOTAL_COUNT_KEY = "totalCount";

    @Autowired
    private CheckinRecordRepository checkinRecordRepository;

    @Autowired
    private ScheduleItemService scheduleItemService;

    @Autowired
    private CheckinRecordMapper checkinRecordMapper;

    @Autowired
    private TransactionFlowService transactionFlowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long performCheckin(CheckinRecordDTO requestDto) {
        if (requestDto.getTaskTime() == null) {
            requestDto.setTaskTime(DateUtil.getStartOfDay(LocalDateTime.now()));
        }
        Long userId = requestDto.getUserId();
        Long taskId = requestDto.getTaskId();
        Long groupId = requestDto.getGroupId();
        Long operatorId = requestDto.getOperatorId();

        ScheduleItemDTO scheduleItemDTO = scheduleItemService.getScheduleItemById(taskId);
        if (scheduleItemDTO == null) {
            throw new RuntimeException("ScheduleItem not found: " + taskId);
        }
        Map<String, Object> dtoExtra = scheduleItemDTO.getExtra();
        logger.info("performCheckin: taskId={}, userId={}, groupId={}, taskExtra={}", taskId, userId, groupId, dtoExtra);

        String taskKey = scheduleItemService.getTaskKey(scheduleItemDTO, requestDto.getTaskTime().toLocalDate());
        int existCount = checkinRecordRepository.countByGroupIdAndUserIdAndTaskKey(groupId, userId, taskKey);
        Integer earnedScore = getInt(dtoExtra, SCORE_KEY);
        if (earnedScore == null) earnedScore = 0;

        // 1. 写 checkin_record
        LocalDateTime completeTime = LocalDateTime.now();
        CheckinRecord record = checkinRecordMapper.toEntity(requestDto);
        record.setCompleteTime(completeTime);
        record.setTaskKey(taskKey);

        JSONObject recordExtra = requestDto.getExtra() == null
                ? new JSONObject()
                : new JSONObject(requestDto.getExtra());
        if (!recordExtra.containsKey("count")) {
            recordExtra.put("count", existCount + 1);
        }
        recordExtra.put("title", scheduleItemDTO.getItemTitle());

        Integer totalCount = getInt(dtoExtra, TOTAL_COUNT_KEY);
        if (totalCount == null) totalCount = 1;
        recordExtra.put("totalCount", totalCount);

        // 只有"刚好达到 totalCount"那次才算最终完成 → 派积分 + 写 updateScope
        boolean justCompleted = totalCount == existCount + 1;
        logger.info("performCheckin scoring decision: taskKey={}, existCount={}, totalCount={}, earnedScore(beforeGate)={}, justCompleted={}",
                taskKey, existCount, totalCount, earnedScore, justCompleted);
        if (justCompleted) {
            ScheduleItemUpdateScope scope = scheduleItemDTO.getUpdateScope();
            if (scope == null) scope = new ScheduleItemUpdateScope();
            scope.setLastCompleteTime(completeTime);
            scheduleItemService.saveForTaskUpdate(scheduleItemDTO.getId(), scope);
        } else {
            earnedScore = 0;
        }
        recordExtra.put(SCORE_KEY, earnedScore);
        record.setTaskTime(requestDto.getTaskTime());
        record.setExtra(JSON.toJSONString(recordExtra));
        CheckinRecord savedRecord = checkinRecordRepository.save(record);
        logger.info("Checkin record created with ID: {}, will award score={}", savedRecord.getId(), earnedScore);

        // 2. 派积分（INCOME 流水）
        if (earnedScore > 0) {
            TransactionFlowDTO flow = buildIncomeFlow(scheduleItemDTO, earnedScore, savedRecord);
            TransactionFlowDTO saved = transactionFlowService.saveTransactionFlow(flow, groupId, userId, operatorId);
            logger.info("TransactionFlow saved: id={}, balance={}", saved.getId(), saved.getBalance());
        } else {
            logger.info("No transaction flow written (earnedScore=0). justCompleted={}, taskScoreInExtra={}",
                    justCompleted, getInt(dtoExtra, SCORE_KEY));
        }
        return savedRecord.getId();
    }

    private static TransactionFlowDTO buildIncomeFlow(ScheduleItemDTO dto, Integer earnedScore, CheckinRecord savedRecord) {
        TransactionFlowDTO flow = new TransactionFlowDTO();
        flow.setFlowType(FlowType.POINTS);
        flow.setAmount(earnedScore);
        flow.setTransactionType(TransactionType.INCOME);
        flow.setDescription("完成打卡：" + dto.getItemTitle());
        JSONObject extra = new JSONObject();
        extra.put("checkinRecordId", savedRecord.getId());
        flow.setExtra(extra);
        return flow;
    }

    private static Integer getInt(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, LocalDateTime from, LocalDateTime to) {
        if (groupId == null || userId == null || from == null || to == null) {
            logger.warn("Invalid parameters for getRecordList: groupId={}, userId={}, from={}, to={}", groupId, userId, from, to);
            return Collections.emptyList();
        }
        List<CheckinRecord> records = checkinRecordRepository.findByGroupIdAndUserIdAndTaskTimeBetween(groupId, userId, from, to);
        return checkinRecordMapper.toDTOList(records);
    }

    @Override
    public List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Long taskId, LocalDate fromDate, LocalDate toDate) {
        if (groupId == null || userId == null || fromDate == null || toDate == null) {
            logger.warn("Invalid parameters for getRecordList(byTaskId): groupId={}, userId={}, taskId={}, from={}, to={}", groupId, userId, taskId, fromDate, toDate);
            return Collections.emptyList();
        }
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atStartOfDay();
        List<CheckinRecord> records = (taskId == null)
                ? checkinRecordRepository.findByGroupIdAndUserIdAndTaskTimeBetween(groupId, userId, from, to)
                : checkinRecordRepository.findByGroupIdAndUserIdAndTaskTimeBetween(groupId, userId, taskId, from, to);
        return checkinRecordMapper.toDTOList(records);
    }

    @Override
    public List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Set<String> taskKeys) {
        if (groupId == null || userId == null || taskKeys == null || taskKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<CheckinRecord> records = checkinRecordRepository.findByGroupIdAndUserIdAndTaskKeyIn(groupId, userId, taskKeys);
        return checkinRecordMapper.toDTOList(records);
    }
}
