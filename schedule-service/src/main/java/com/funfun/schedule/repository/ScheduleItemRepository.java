package com.funfun.schedule.repository;

import com.funfun.schedule.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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



    List<ScheduleItem> findByIdIn(List<Long> ids);
    List<ScheduleItem> findByParentIdIn(List<Long> parentIds);
    /**
     * 查找在指定时间窗口内有活动（重复周期与窗口重叠）且属于指定组的日程项。
     * 时间重叠条件：schedule.repeatStartDay < windowEndTime AND schedule.repeatEndDay > windowStartTime
     *
     * @param groupId        组ID
     * @param windowStartTime 查询时间窗口的开始时间
     * @param windowEndTime   查询时间窗口的结束时间
     * @return 符合条件的日程项列表
     */
    @Query("SELECT s FROM ScheduleItem s WHERE s.groupId = :groupId " +
            "AND s.repeatStartDay <= :windowEndTime " +
            "AND s.itemType = :itemType "+
            "AND s.repeatEndDay >= :windowStartTime")
    List<ScheduleItem> findOverlappingByGroupId(
            @Param("itemType") String itemType,
            @Param("groupId") Long groupId,
            @Param("windowStartTime") LocalDate windowStartTime,
            @Param("windowEndTime") LocalDate windowEndTime);


    @Query("SELECT s FROM ScheduleItem s WHERE s.userId = :userId " +
            "AND s.repeatStartDay <= :windowEndTime " +
            "AND s.itemType = :itemType "+
            "AND s.repeatEndDay >= :windowStartTime")
    List<ScheduleItem> findOverlappingByUserId(
            @Param("itemType") String itemType,
            @Param("userId") Long userId,
            @Param("windowStartTime") LocalDate windowStartTime,
            @Param("windowEndTime") LocalDate windowEndTime);



    @Query("SELECT s FROM ScheduleItem s WHERE " +
            "s.userId = :userId " +
            "AND s.itemType = :itemType "+
            "AND s.groupId = :groupId "+
            "AND s.repeatStartDay <= :windowEndTime " +
            "AND s.repeatEndDay >= :windowStartTime")
    List<ScheduleItem> findOverlappingByGroupIdAndUserId(

            @Param("itemType") String itemType,
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("windowStartTime") LocalDate windowStartTime,
            @Param("windowEndTime") LocalDate windowEndTime);

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