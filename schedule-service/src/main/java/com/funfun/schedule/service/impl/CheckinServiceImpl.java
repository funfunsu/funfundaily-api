package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.dto.TransactionFlowDTO;
import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.enums.TaskType;
import com.funfun.schedule.enums.TransactionType;
import com.funfun.schedule.exception.CommonException;
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

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CheckinServiceImpl implements CheckinService {

    private static final Logger logger = LoggerFactory.getLogger(CheckinServiceImpl.class);

    @Autowired
    private CheckinRecordRepository checkinRecordRepository;

    @Autowired
    private ScheduleItemService scheduleItemService;

    @Autowired
    private CheckinRecordMapper checkinRecordMapper;

    @Autowired
    private TransactionFlowService transactionFlowService;

    private static final String scoreKey = "score";
    private static final String taskTypeKey = "taskType";
    private static final String totalCntKey = "totalCount";


    private Integer getScore(JSONObject extraMap){
        if (extraMap == null){
            return 0;
        }
        return extraMap.getInteger(scoreKey);
    }

    @Override
    @Transactional // 确保打卡和积分记录在同一事务中
    public Long performCheckin(CheckinRecordDTO requestDto) {
        if (requestDto.getTaskTime() == null){
            requestDto.setTaskTime(DateUtil.getStartOfDay(LocalDateTime.now()));
        }

        Long userId = requestDto.getUserId(); // 从安全上下文获取更佳
        Long taskId = requestDto.getTaskId();
        Long groupId = requestDto.getGroupId();
        Long operatorId = requestDto.getOperatorId(); // 操作人，通常是用户自己

        ScheduleItemDTO scheduleItemDTO = scheduleItemService.getScheduleItemById(taskId);
        String taskType = scheduleItemDTO.getExtra() == null? null :scheduleItemDTO.getExtra().getString(taskTypeKey);
        String taskKey = scheduleItemService.getTaskKey(scheduleItemDTO,requestDto.getTaskTime().toLocalDate());
        int existCount = checkinRecordRepository.countByGroupIdAndUserIdAndTaskKey(groupId,userId, taskKey);
        Integer earnedScore = getScore(scheduleItemDTO.getExtra()); // 示例固定积分


        // --- 2. 创建打卡记录 ---
        LocalDateTime completeTime = LocalDateTime.now();
        CheckinRecord checkinRecord = checkinRecordMapper.toEntity(requestDto);
        checkinRecord.setCompleteTime(completeTime);
        checkinRecord.setTaskKey(taskKey);
        JSONObject recordExtra = requestDto.getExtra();
        if(recordExtra == null){
            recordExtra =  new JSONObject();
            recordExtra.put("count",existCount+1);
        }
        recordExtra.put("title",scheduleItemDTO.getItemTitle());

        boolean completeFlag;
        Integer totalCount = scheduleItemDTO.getExtra().getInteger(totalCntKey);
        recordExtra.put("totalCount",totalCount);
        //只有== 的时候更新，后面多打了不更新
        completeFlag = totalCount == existCount + 1;

        // --- 更新任务信息
        if (completeFlag){
            ScheduleItemUpdateScope scheduleItemUpdateScope = scheduleItemDTO.getUpdateScope();
            if (scheduleItemUpdateScope == null){
                scheduleItemUpdateScope = new ScheduleItemUpdateScope();
            }
            scheduleItemUpdateScope.setLastCompleteTime(completeTime);
            scheduleItemService.saveForTaskUpdate(scheduleItemDTO.getId(),scheduleItemUpdateScope);
        }else{
            earnedScore = 0;
        }
        recordExtra.put("score",earnedScore);
        checkinRecord.setTaskTime(requestDto.getTaskTime());
        checkinRecord.setExtra(JSON.toJSONString(recordExtra));
        // 如果需要记录额外信息，可以设置 extra 字段
        CheckinRecord savedRecord = checkinRecordRepository.save(checkinRecord);
        logger.info("Checkin record created with ID: {}", savedRecord.getId());

        // --- 3. 计算积分 (示例) ---
        if (earnedScore > 0){
            TransactionFlowDTO flow = getFlowDTO(scheduleItemDTO, earnedScore, savedRecord);
            transactionFlowService.saveTransactionFlow(flow,groupId,userId,operatorId);
        }
        return savedRecord.getId();


    }

    private static TransactionFlowDTO getFlowDTO(ScheduleItemDTO scheduleItemDTO, Integer earnedScore, CheckinRecord savedRecord) {
        String eventName = scheduleItemDTO.getItemTitle();
        TransactionFlowDTO flow = new TransactionFlowDTO();
        flow.setFlowType(FlowType.POINTS);
        flow.setAmount(earnedScore);
        flow.setTransactionType(TransactionType.INCOME);
        flow.setDescription("完成打卡："+eventName);
        JSONObject extra = new JSONObject();
        extra.put("checkinRecordId", savedRecord.getId());
        flow.setExtra(extra);
        return flow;
    }

    @Override
    public List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Set<String> taskKeys) {
        List<CheckinRecord> records = checkinRecordRepository.findByGroupIdAndUserIdAndTaskKeyIn(groupId,userId,taskKeys);
        return checkinRecordMapper.toDTOList(records);
    }

    @Override
    public List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Long taskId, LocalDate fromDate, LocalDate toDate) {
         // --- 参数校验 (可选但推荐) ---
        if (groupId == null || userId == null || fromDate == null || toDate == null) {
            logger.warn("Invalid parameters for getRecordList: groupId={}, userId={}, from={}, to={}", groupId, userId, fromDate, fromDate);
            // 可以抛出 IllegalArgumentException 或返回空列表
            return List.of(); // 返回空列表
            // throw new IllegalArgumentException("Parameters cannot be null");
        }
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atStartOfDay();
        // --- 调用 Repository 查询 ---
        if (taskId == null){
            List<CheckinRecord> records = checkinRecordRepository.findByGroupIdAndUserIdAndTaskTimeBetween(groupId, userId, from, to);
            return checkinRecordMapper.toDTOList(records);
        }else{
            List<CheckinRecord> records = checkinRecordRepository.findByGroupIdAndUserIdAndTaskTimeBetween(groupId, userId, taskId,from, to);
            return checkinRecordMapper.toDTOList(records);
        }

    }
}