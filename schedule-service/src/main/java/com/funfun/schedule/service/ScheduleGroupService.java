package com.funfun.schedule.service;

import com.funfun.schedule.dto.GroupDTO;
import com.funfun.schedule.entity.Group;
import java.util.Date;
import java.util.List;

/**
 * ScheduleGroupService接口，定义群组相关的业务逻辑方法
 */
public interface ScheduleGroupService {

    /**
     * 创建群组
     * @param group 群组对象
     * @return 创建后的群组对象
     */
    Group createGroup(GroupDTO group);
    Group createAutoGroup(Long userId);
    Group removeAutoGroup(Long userId);
    Group getAutoGroup(Long userId);

    /**
     * 根据ID查询群组
     * @param id 群组ID
     * @return 群组对象
     */
    Group getGroupById(Long id);

    /**
     * 查询所有群组
     * @return 群组列表
     */
    List<Group> getGroupList(Long userId);
    int involvedGroupCount(Long userId);

    /**
     * 根据创建者ID查询群组
     * @param creator 创建者ID
     * @return 群组列表
     */
    List<Group> getGroupsByCreator(Long creator);

    /**
     * 根据标题模糊查询群组
     * @param title 群组标题
     * @return 群组列表
     */
    List<Group> getGroupsByTitleContaining(String title);

    /**
     * 根据创建时间范围查询群组
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 群组列表
     */
    List<Group> getGroupsByCreateTimeBetween(Date startDate, Date endDate);

    /**
     * 更新群组信息
     * @param groupDTO 群组对象
     * @return 更新后的群组对象
     */
    Group updateGroup(GroupDTO groupDTO);

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
    List<Group> getGroupsByIds(List<Long> ids);

    /**
     * 根据标题和创建者查询群组
     * @param title 群组标题
     * @param creator 创建者ID
     * @return 群组对象
     */
    Group getGroupByTitleAndCreator(String title, Long creator);
}