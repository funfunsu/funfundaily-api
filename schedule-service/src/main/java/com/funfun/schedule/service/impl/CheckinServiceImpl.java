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

import jakarta.transaction.Transactional;
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
    // 积分模式：each=单次打卡即得分；full（默认/缺省）=全部完成才得分
    private static final String scoreModeKey = "scoreMode";
    private static final String SCORE_MODE_EACH = "each";
    // 每日打卡上限：>=1 时校验当天已打卡次数；缺省/<=0 表示不限制（兼容历史数据）
    private static final String dailyLimitKey = "dailyLimit";
    // 周期全部完成额外奖励积分：>0 时在周期达成那一次额外发放
    private static final String bonusScoreKey = "bonusScore";


    private Integer getScore(JSONObject extraMap){
        if (extraMap == null || extraMap.getInteger(scoreKey) == null){
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
        JSONObject itemExtra = scheduleItemDTO.getExtra();
        String taskKey = scheduleItemService.getTaskKey(scheduleItemDTO,requestDto.getTaskTime().toLocalDate());
        int existCount = checkinRecordRepository.countByGroupIdAndUserIdAndTaskKey(groupId,userId, taskKey);
        Integer earnedScore = getScore(itemExtra); // 任务配置的单次积分

        // --- 1. 每日打卡上限校验 ---
        // dailyLimit>=1：限制同一自然日内该任务的打卡次数；缺省/<=0 视为不限制（兼容历史任务）
        Integer dailyLimit = itemExtra == null ? null : itemExtra.getInteger(dailyLimitKey);
        if (dailyLimit != null && dailyLimit >= 1) {
            LocalDateTime dayStart = DateUtil.getStartOfDay(requestDto.getTaskTime());
            LocalDateTime dayEnd = dayStart.plusDays(1);
            int todayCount = checkinRecordRepository
                    .findByGroupIdAndUserIdAndTaskTimeBetween(groupId, userId, taskId, dayStart, dayEnd)
                    .size();
            if (todayCount >= dailyLimit) {
                CommonException.NOT_ALLOWED.throwsError("今日打卡次数已达上限（" + dailyLimit + " 次），明天再来吧");
            }
        }


        // --- 2. 创建打卡记录 ---
        LocalDateTime completeTime = LocalDateTime.now();
        CheckinRecord checkinRecord = checkinRecordMapper.toEntity(requestDto);
        checkinRecord.setCompleteTime(completeTime);
        checkinRecord.setTaskKey(taskKey);
        JSONObject recordExtra = requestDto.getExtra();
        if(recordExtra == null){
            recordExtra =  new JSONObject();
        }
        int checkinSeq = existCount + 1; // 本次为当前周期内第几次打卡
        recordExtra.put("count", checkinSeq);
        recordExtra.put("title",scheduleItemDTO.getItemTitle());

        Integer totalCount = itemExtra == null ? null : itemExtra.getInteger(totalCntKey);
        recordExtra.put("totalCount",totalCount);
        // 本次打卡是否使整个周期达成（只有刚好达到 totalCount 的那一次为 true，后面多打不再触发）
        boolean completeFlag = totalCount != null && totalCount == existCount + 1;

        // --- 更新任务信息：周期达成时记录最近完成时间 ---
        if (completeFlag){
            ScheduleItemUpdateScope scheduleItemUpdateScope = scheduleItemDTO.getUpdateScope();
            if (scheduleItemUpdateScope == null){
                scheduleItemUpdateScope = new ScheduleItemUpdateScope();
            }
            scheduleItemUpdateScope.setLastCompleteTime(completeTime);
            scheduleItemService.saveForTaskUpdate(scheduleItemDTO.getId(),scheduleItemUpdateScope);
        }

        // --- 积分模式：each=每次打卡都得分；full（默认）=仅周期全部完成才得分 ---
        String scoreMode = itemExtra == null ? null : itemExtra.getString(scoreModeKey);
        boolean awardEachCheckin = SCORE_MODE_EACH.equals(scoreMode);
        if (!awardEachCheckin && !completeFlag){
            earnedScore = 0;
        }

        // --- 周期全部完成额外奖励：仅在达成那一次发放（与单次/完成积分叠加）---
        int bonusScore = 0;
        if (completeFlag && itemExtra != null){
            Integer bonus = itemExtra.getInteger(bonusScoreKey);
            if (bonus != null && bonus > 0){
                bonusScore = bonus;
            }
        }

        recordExtra.put("score",earnedScore);
        if (bonusScore > 0){
            recordExtra.put("bonusScore", bonusScore);
        }
        checkinRecord.setTaskTime(requestDto.getTaskTime());
        checkinRecord.setExtra(JSON.toJSONString(recordExtra));
        // 如果需要记录额外信息，可以设置 extra 字段
        CheckinRecord savedRecord = checkinRecordRepository.save(checkinRecord);
        logger.info("Checkin record created with ID: {}", savedRecord.getId());

        // --- 积分流水：单次/完成积分 ---
        String eventName = scheduleItemDTO.getItemTitle();
        if (earnedScore > 0){
            // 每次打卡得分时备注「第N次打卡」，便于在流水中区分同一周期的多次打卡
            String desc = "完成打卡：" + eventName + (awardEachCheckin ? " 第" + checkinSeq + "次打卡" : "");
            transactionFlowService.saveTransactionFlow(getFlowDTO(earnedScore, desc, savedRecord), groupId, userId, operatorId);
        }
        // --- 积分流水：周期全部完成额外奖励 ---
        if (bonusScore > 0){
            String bonusDesc = eventName + " 周期内任务全部完成额外奖励";
            transactionFlowService.saveTransactionFlow(getFlowDTO(bonusScore, bonusDesc, savedRecord), groupId, userId, operatorId);
        }
        return savedRecord.getId();


    }

    @Override
    @Transactional
    public Long performAbstainFeedback(CheckinRecordDTO requestDto) {
        if (requestDto.getTaskTime() == null) {
            requestDto.setTaskTime(DateUtil.getStartOfDay(LocalDateTime.now()));
        }
        Long userId = requestDto.getUserId();
        Long taskId = requestDto.getTaskId();
        Long groupId = requestDto.getGroupId();

        ScheduleItemDTO scheduleItemDTO = scheduleItemService.getScheduleItemById(taskId);
        if (scheduleItemDTO == null) {
            CommonException.DATA_INVALID.throwsError("戒断事件不存在");
        }

        // 戒断反馈按天归属：taskKey = taskId:yyyy-MM-dd（绕开 getTaskKey 对 repeatType=none 的空周期键问题）
        LocalDate day = requestDto.getTaskTime().toLocalDate();
        String taskKey = taskId + ":" + day;

        JSONObject extra = requestDto.getExtra() == null ? new JSONObject() : requestDto.getExtra();
        extra.put("title", scheduleItemDTO.getItemTitle());

        // 同一天再次反馈：覆盖原记录（允许达成↔破戒互改）
        CheckinRecord record = checkinRecordRepository
                .findFirstByGroupIdAndUserIdAndTaskKeyOrderByCompleteTimeDesc(groupId, userId, taskKey)
                .orElseGet(CheckinRecord::new);
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setGroupId(groupId);
        record.setTaskKey(taskKey);
        record.setTaskTime(DateUtil.getStartOfDay(requestDto.getTaskTime()));
        record.setCompleteTime(LocalDateTime.now());
        record.setExtra(JSON.toJSONString(extra));
        record.setDeleted(false);
        CheckinRecord saved = checkinRecordRepository.save(record);
        logger.info("Abstain feedback saved id={}, taskKey={}, feedback={}", saved.getId(), taskKey, extra.get("feedback"));
        return saved.getId();
    }

    private static TransactionFlowDTO getFlowDTO(Integer earnedScore, String description, CheckinRecord savedRecord) {
        TransactionFlowDTO flow = new TransactionFlowDTO();
        flow.setFlowType(FlowType.POINTS);
        flow.setAmount(earnedScore);
        flow.setTransactionType(TransactionType.INCOME);
        flow.setDescription(description);
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