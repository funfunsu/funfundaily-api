package com.funfun.schedule.service;

import com.funfun.schedule.entity.ScheduleItem;

import java.util.List;
import java.util.Optional;

/**
 * ScheduleItemService接口，定义ScheduleItem相关的业务逻辑方法
 */
public interface ScheduleItemService {

    /**
     * 创建日程项
     * @param scheduleItem 日程项对象
     * @return 创建的日程项对象
     */
    ScheduleItem createScheduleItem(ScheduleItem scheduleItem);

    /**
     * 根据ID查询日程项
     * @param id 日程项ID
     * @return 日程项对象（Optional包装）
     */
    Optional<ScheduleItem> getScheduleItemById(Integer id);

    /**
     * 查询所有日程项
     * @return 日程项列表
     */
    List<ScheduleItem> getAllScheduleItems();

    /**
     * 更新日程项
     * @param id 日程项ID
     * @param scheduleItem 新的日程项数据
     * @return 更新后的日程项对象
     */
    ScheduleItem updateScheduleItem(Integer id, ScheduleItem scheduleItem);

    /**
     * 删除日程项
     * @param id 日程项ID
     */
    void deleteScheduleItem(Integer id);

    /**
     * 根据groupId和personId查询日程项
     * @param groupId 组ID
     * @param personId 人员ID
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByGroupIdAndPersonId(Integer groupId, Integer personId);

    /**
     * 根据groupId查询日程项
     * @param groupId 组ID
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByGroupId(Integer groupId);

    /**
     * 根据personId查询日程项
     * @param personId 人员ID
     * @return 日程项列表
     */
    List<ScheduleItem> getScheduleItemsByPersonId(Integer personId);

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
    void batchDeleteScheduleItems(List<Integer> ids);
}