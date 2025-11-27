package com.funfun.schedule.repository;

import com.funfun.schedule.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * CheckinRecordRepository接口，用于CheckinRecord实体的数据库操作
 */
@Repository
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    /**
     * 根据群组ID和用户ID查询签到记录
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 签到记录列表
     */
    List<CheckinRecord> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 根据任务ID查询签到记录
     * @param taskId 任务ID
     * @return 签到记录列表
     */
    List<CheckinRecord> findByTaskId(Long taskId);

    /**
     * 根据任务ID和用户ID查询签到记录
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 签到记录对象（Optional包装）
     */
    Optional<CheckinRecord> findByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * 根据完成状态查询签到记录
     * @param completeStatus 完成状态
     * @return 签到记录列表
     */
    List<CheckinRecord> findByCompleteStatus(Integer completeStatus);

    /**
     * 根据完成时间范围查询签到记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 签到记录列表
     */
    List<CheckinRecord> findByCompleteTimeBetween(Date startDate, Date endDate);

    /**
     * 根据删除标志查询签到记录
     * @param deleteFlag 删除标志
     * @return 签到记录列表
     */
    List<CheckinRecord> findByDeleteFlag(Integer deleteFlag);

    /**
     * 批量查询签到记录
     * @param ids 记录ID列表
     * @return 签到记录列表
     */
    List<CheckinRecord> findByIdIn(List<Long> ids);

    /**
     * 判断是否存在指定任务和用户的签到记录
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * 批量删除签到记录
     * @param ids 记录ID列表
     */
    void deleteByIdIn(List<Long> ids);
}