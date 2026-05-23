package com.funfun.schedule.repository;

import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.CloseStatus;
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

    /** 邀请函：某用户在某群下、指定类型、自己创建的记录（用于「我发出的邀请」列表）。 */
    List<ScheduleItem> findByGroupIdAndCreateByAndItemTypeOrderByCreateTimeDesc(
            Long groupId, Long createBy, String itemType);

    /** 邀请函：某用户持有的、指定类型的记录（用于「我收到的邀请」列表，不限群组）。 */
    List<ScheduleItem> findByUserIdAndItemTypeOrderByCreateTimeDesc(Long userId, String itemType);

    /** 邀请函：某用户对某个原邀请（parentId）已收下的记录（用于幂等判断）。 */
    List<ScheduleItem> findByParentIdAndUserIdAndItemType(Long parentId, Long userId, String itemType);

    /** 邀请函：某个原邀请的所有子记录（用于级联更新）。 */
    List<ScheduleItem> findByParentIdAndItemType(Long parentId, String itemType);

    /**
     * 查询某成员在某组下、指定类型、指定关闭状态的日程项（用于「已停止关注」列表，不做日期窗口过滤）。
     */
    List<ScheduleItem> findByGroupIdAndUserIdAndItemTypeAndCloseStatus(
            Long groupId, Long userId, String itemType, CloseStatus closeStatus);
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
            "AND s.closeStatus <> com.funfun.schedule.enums.CloseStatus.CLOSE "+
            "AND s.repeatEndDay >= :windowStartTime")
    List<ScheduleItem> findOverlappingByGroupId(
            @Param("itemType") String itemType,
            @Param("groupId") Long groupId,
            @Param("windowStartTime") LocalDate windowStartTime,
            @Param("windowEndTime") LocalDate windowEndTime);


    @Query("SELECT s FROM ScheduleItem s WHERE s.userId = :userId " +
            "AND s.repeatStartDay <= :windowEndTime " +
            "AND s.itemType = :itemType "+
            "AND s.closeStatus <> com.funfun.schedule.enums.CloseStatus.CLOSE "+
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
            "AND s.closeStatus <> com.funfun.schedule.enums.CloseStatus.CLOSE "+
            "AND s.repeatStartDay <= :windowEndTime " +
            "AND s.repeatEndDay >= :windowStartTime")
    List<ScheduleItem> findOverlappingByGroupIdAndUserId(

            @Param("itemType") String itemType,
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("windowStartTime") LocalDate windowStartTime,
            @Param("windowEndTime") LocalDate windowEndTime);

    /**
     * 开放接口（OpenAPI / MCP）专用：查询某群组下、指定类型、未关闭的任务，
     * 支持按 userId / parentId 可选过滤，并按创建时间升序返回。
     *
     * <p>groupId 为必填（来自令牌绑定，用于数据隔离）；userId、parentId 传 null 时不参与过滤。
     *
     * @param groupId  群组 ID（数据隔离边界，必填）
     * @param itemType 项目类型，通常为 "task"
     * @param userId   成员 ID（可选过滤）
     * @param parentId 父任务 ID（可选过滤）
     * @return 按 createTime 升序排列的任务列表
     */
    @Query("SELECT s FROM ScheduleItem s WHERE s.groupId = :groupId " +
            "AND s.itemType = :itemType " +
            "AND s.closeStatus <> com.funfun.schedule.enums.CloseStatus.CLOSE " +
            "AND (:userId IS NULL OR s.userId = :userId) " +
            "AND (:parentId IS NULL OR s.parentId = :parentId) " +
            "ORDER BY s.createTime ASC")
    List<ScheduleItem> findOpenTasksForOpenApi(
            @Param("groupId") Long groupId,
            @Param("itemType") String itemType,
            @Param("userId") Long userId,
            @Param("parentId") Long parentId);

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