package com.funfun.schedule.repository;

import com.funfun.schedule.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * ScheduleGroupRepository接口，用于ScheduleGroup实体的数据库操作
 */
@Repository
public interface ScheduleGroupRepository extends JpaRepository<Group, Long> {

    /**
     * 根据创建者ID查询群组
     * @param creator 创建者ID
     * @return 群组列表
     */
    List<Group> findByCreator(Long creator);
    List<Group> findByCreatorAndType(Long creator,int Type);

    List<Group> findByGroupNameContaining(String groupName);

    /**
     * 根据创建时间范围查询群组
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 群组列表
     */
    List<Group> findByCreateTimeBetween(Date startDate, Date endDate);

    /**
     * 根据群组ID列表批量查询群组
     * @param ids 群组ID列表
     * @return 群组列表
     */
    List<Group> findByIdIn(List<Long> ids);

    /**
     * 根据标题和创建者查询群组
     * @param itemTitle 群组标题
     * @param creator 创建者ID
     * @return 群组对象（Optional包装）
     */
    Optional<Group> findByGroupNameAndCreator(String itemTitle, Long creator);

    /**
     * 批量删除群组
     * @param ids 群组ID列表
     */
    void deleteByIdIn(List<Long> ids);
}