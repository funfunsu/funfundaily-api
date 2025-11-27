package com.funfun.schedule.repository;

import com.funfun.schedule.entity.ScoreFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * ScoreFlowRepository接口，用于ScoreFlow实体的数据库操作
 */
@Repository
public interface ScoreFlowRepository extends JpaRepository<ScoreFlow, Long> {

    /**
     * 根据群组ID和用户ID查询积分流水
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 积分流水列表
     */
    List<ScoreFlow> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 根据用户ID查询积分流水
     * @param userId 用户ID
     * @return 积分流水列表
     */
    List<ScoreFlow> findByUserId(Long userId);

    /**
     * 根据群组ID查询积分流水
     * @param groupId 群组ID
     * @return 积分流水列表
     */
    List<ScoreFlow> findByGroupId(Long groupId);

    /**
     * 根据事件名称查询积分流水
     * @param eventName 事件名称
     * @return 积分流水列表
     */
    List<ScoreFlow> findByEventName(String eventName);

    /**
     * 根据创建时间范围查询积分流水
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 积分流水列表
     */
    List<ScoreFlow> findByCreateTimeBetween(Date startDate, Date endDate);

    /**
     * 根据操作人ID查询积分流水
     * @param operator 操作人ID
     * @return 积分流水列表
     */
    List<ScoreFlow> findByOperator(Long operator);

    /**
     * 根据删除标志查询积分流水
     * @param deleteFlag 删除标志
     * @return 积分流水列表
     */
    List<ScoreFlow> findByDeleteFlag(Integer deleteFlag);

    /**
     * 批量查询积分流水
     * @param ids 记录ID列表
     * @return 积分流水列表
     */
    List<ScoreFlow> findByIdIn(List<Long> ids);

    /**
     * 批量删除积分流水
     * @param ids 记录ID列表
     */
    void deleteByIdIn(List<Long> ids);
}