package com.funfun.schedule.service;

import com.funfun.schedule.entity.ScheduleGroup;
import java.util.Date;
import java.util.List;

/**
 * ScheduleGroupService接口，定义群组相关的业务逻辑方法
 */
public interface ScheduleGroupService {

    /**
     * 创建群组
     * @param scheduleGroup 群组对象
     * @return 创建后的群组对象
     */
    ScheduleGroup createGroup(ScheduleGroup scheduleGroup);

    /**
     * 根据ID查询群组
     * @param id 群组ID
     * @return 群组对象
     */
    ScheduleGroup getGroupById(Long id);

    /**
     * 查询所有群组
     * @return 群组列表
     */
    List<ScheduleGroup> getAllGroups();

    /**
     * 根据创建者ID查询群组
     * @param creator 创建者ID
     * @return 群组列表
     */
    List<ScheduleGroup> getGroupsByCreator(Long creator);

    /**
     * 根据标题模糊查询群组
     * @param title 群组标题
     * @return 群组列表
     */
    List<ScheduleGroup> getGroupsByTitleContaining(String title);

    /**
     * 根据创建时间范围查询群组
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 群组列表
     */
    List<ScheduleGroup> getGroupsByCreateTimeBetween(Date startDate, Date endDate);

    /**
     * 更新群组信息
     * @param scheduleGroup 群组对象
     * @return 更新后的群组对象
     */
    ScheduleGroup updateGroup(ScheduleGroup scheduleGroup);

    /**
     * 删除群组
     * @param id 群组ID
     */
    void deleteGroup(Long id);

    /**
     * 批量删除群组
     * @param ids 群组ID列表
     */
    void deleteGroups(List<Long> ids);

    /**
     * 批量查询群组
     * @param ids 群组ID列表
     * @return 群组列表
     */
    List<ScheduleGroup> getGroupsByIds(List<Long> ids);

    /**
     * 根据标题和创建者查询群组
     * @param title 群组标题
     * @param creator 创建者ID
     * @return 群组对象
     */
    ScheduleGroup getGroupByTitleAndCreator(String title, Long creator);
}