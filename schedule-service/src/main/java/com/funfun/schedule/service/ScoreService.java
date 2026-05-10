package com.funfun.schedule.service;

import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.entity.ScoreFlow;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ScoreService接口，定义积分相关的业务逻辑方法
 */
public interface ScoreService {

    /**
     * 用户完成任务后派发积分
     * @param checkinRecord 签到记录（包含任务、用户、群组等信息）
     * @param score 派发的积分数量
     * @param operator 操作人ID
     * @param extInfo 扩展信息
     * @return 积分流水记录
     */
    ScoreFlow distributeScoreForTask(CheckinRecord checkinRecord, Integer score, Long operator, Map<String, Object> extInfo);

    /**
     * 积分兑换（减少积分）
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param score 兑换的积分数量
     * @param eventName 事件名称
     * @param operator 操作人ID
     * @param extInfo 扩展信息
     * @return 积分流水记录
     */
    ScoreFlow redeemScore(Long userId, Long groupId, Integer score, String eventName, Long operator, Map<String, Object> extInfo);

    /**
     * 查询用户的积分余额
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 积分余额
     */
    Integer getUserScoreBalance(Long userId, Long groupId);

    /**
     * 查询用户的积分流水
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 积分流水列表
     */
    List<ScoreFlow> getUserScoreFlows(Long userId, Long groupId, Date startDate, Date endDate);

    /**
     * 根据ID查询积分流水
     * @param id 积分流水ID
     * @return 积分流水记录
     */
    ScoreFlow getScoreFlowById(Long id);

    /**
     * 批量查询积分流水
     * @param ids 积分流水ID列表
     * @return 积分流水列表
     */
    List<ScoreFlow> getScoreFlowsByIds(List<Long> ids);

    /**
     * 检查用户积分是否足够
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param requiredScore 需要的积分数量
     * @return 是否足够
     */
    boolean checkScoreSufficient(Long userId, Long groupId, Integer requiredScore);
}