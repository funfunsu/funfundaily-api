package com.funfun.schedule.repository;

import com.funfun.schedule.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ScheduleItemRepository接口，用于ScheduleItem实体的数据库操作
 */
@Repository
public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    /**
     * 根据groupId和UserId查询日程项
     * @param groupId 组ID
     * @param UserId 人员ID
     * @return 日程项列表
     */
    List<ScheduleItem> findByGroupIdAndUserId(Long groupId, Long UserId);

    /**
     * 根据groupId查询日程项
     * @param groupId 组ID
     * @return 日程项列表
     */
    List<ScheduleItem> findByGroupId(Long groupId);

    /**
     * 根据UserId查询日程项
     * @param UserId 人员ID
     * @return 日程项列表
     */
    List<ScheduleItem> findByUserId(Long UserId);

    /**
     * 根据itemType查询日程项
     * @param itemType 项目类型
     * @return 日程项列表
     */
    List<ScheduleItem> findByItemType(String itemType);

    /**
     * 根据repeatType查询日程项
     * @param repeatType 重复类型
     * @return 日程项列表
     */
    List<ScheduleItem> findByRepeatType(String repeatType);

    /**
     * 根据groupId和UserId删除日程项
     * @param groupId 组ID
     * @param userId 人员ID
     */
    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 根据groupId删除日程项
     * @param groupId 组ID
     */
    void deleteByGroupId(Long groupId);

    /**
     * 根据UserId删除日程项
     * @param userId 人员ID
     */
    void deleteByUserId(Long userId);
}