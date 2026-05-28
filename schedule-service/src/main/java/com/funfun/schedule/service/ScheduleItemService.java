package com.funfun.schedule.service;

import com.funfun.schedule.dto.QueryScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.enums.ScheduleItemType;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ScheduleItemService接口，定义ScheduleItem相关的业务逻辑方法
 */
public interface ScheduleItemService {

    /**
     * 创建日程项
     * @param scheduleItem 日程项对象
     * @return 创建的日程项对象
     */
    boolean createScheduleItems(Long userId, Long groupId, Long targetUserId, List<ScheduleItemDTO> scheduleItem);

    /**
     * 根据ID查询日程项
     * @param id 日程项ID
     * @return 日程项对象（Optional包装）
     */
    ScheduleItemDTO getScheduleItemById(Long id);
    /**
     * 更新日程项
     * @param id 日程项ID
     * @param scheduleItem 新的日程项数据
     * @return 更新后的日程项对象
     */
    ScheduleItem updateScheduleItem(Long id, ScheduleItem scheduleItem);

    String getTaskKey(ScheduleItemDTO scheduleItemDTO, LocalDate taskTime);
    ScheduleItem saveForTaskUpdate(Long id, ScheduleItemUpdateScope updateScope);

    /**
     * 设置日程项关注状态（停止关注 / 恢复关注）。
     * @param id 日程项ID
     * @param closeStatus CLOSE=停止关注，OPEN=恢复关注
     */
    ScheduleItem updateCloseStatus(Long id, CloseStatus closeStatus);

    /**
     * 查询某成员在某组下、指定类型、已停止关注（CLOSE）的日程项扁平列表（用于恢复入口）。
     */
    List<ScheduleItemDTO> getClosedItems(Long groupId, Long userId, ScheduleItemType itemType);

    /**
     * 查询某成员在某组下、指定类型、未停止关注（OPEN）的日程项扁平列表（不按天展开）。
     * 用于戒断事件等「长期持续事件」的事件列表。
     */
    List<ScheduleItemDTO> getActiveItems(Long groupId, Long userId, ScheduleItemType itemType);

    /**
     * 删除日程项
     * @param id 日程项ID
     */
    void deleteScheduleItem(Long id);

    /**
     * 根据groupId和personId查询日程项
     * @param groupId 组ID
     * @param userId 人员ID
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByGroupIdAndPersonId(Long groupId, Long userId);

    /**
     * 根据groupId查询日程项
     * @param groupId 组ID
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByGroupId(Long groupId);

    /**
     * 根据personId查询日程项
     * @param userId 人员ID
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByPersonId(Long userId);

    /**
     * 根据itemType查询日程项
     * @param itemType 项目类型
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByItemType(String itemType);

    /**
     * 根据repeatType查询日程项
     * @param repeatType 重复类型
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByRepeatType(String repeatType);

    /**
     * 批量创建日程项
     * @param scheduleItems 日程项列表
     * @return 创建的日程项列表
     */
    List<ScheduleItem> batchCreateScheduleItems(List<ScheduleItem> scheduleItems);

    /**
     * 批量删除日程项
     * @param ids 日程项ID列表
     */
    void batchDeleteScheduleItems(List<Long> ids);


    List<ScheduleItemDTO> getItemList(List<Long> taskIds);
    List<ScheduleItemDTO> getItemListByParentIds(List<Long> parentIds);

    /**
     * 月度计划：查询某群组下、指定类型、未关闭的全部项（原始列表，不按天展开）。
     * 由前端按月份归属判定一次性 / 周期性事件。
     * @param groupId 组ID
     * @param itemType 项目类型（monthlyPlan）
     * @return 原始日程项 DTO 列表
     */
    List<ScheduleItemDTO> getPlanItems(Long groupId, ScheduleItemType itemType);

    /**
     * 根据groupId、userId、起始日期和结束日期查询日程项，并按日期分组
     * @param groupId 组ID
     * @param userId 人员ID
     * @param fromDate 起始日期
     * @param toDate 结束日期
     * @return 按日期分组的日程项Map
     */
    List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, LocalDate fromDate, LocalDate toDate, ScheduleItemType scheduleItemType);
    List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, QueryScheduleItemDTO queryScheduleItemDTO) throws ParseException;


    List<ScheduleListItemDTO> transferToDateScheduleItems(ScheduleItemType scheduleItemType,String fromDate, String toDate,List<ScheduleItemDTO> list) throws ParseException;
}