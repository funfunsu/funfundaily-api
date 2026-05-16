package com.funfun.schedule.repository;

import com.funfun.schedule.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * GroupMemberRepository接口，用于GroupMember实体的数据库操作
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /**
     * 根据群组ID查询群组成员
     * @param groupId 群组ID
     * @return 群组成员列表
     */
    List<GroupMember> findByGroupId(Long groupId);

    long countByUserId(Long userId); // 新增方法
    long countByGroupId(Long groupId); // 新增方法

    /**
     * 根据用户ID查询该用户加入的群组
     * @param userId 用户ID
     * @return 群组成员列表（每个元素代表一个群组的成员关系）
     */
    List<GroupMember> findByUserId(Long userId);

    /**
     * 根据群组ID和用户ID查询群组成员关系
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 群组成员对象（Optional包装）
     */
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 加锁查询群组成员，保证积分扣减时的并发安全。
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 群组成员对象
     */
    @Query(value = "select * from group_member where group_id = :groupId and user_id = :userId and delete_flag = 0 limit 1 for update", nativeQuery = true)
    Optional<GroupMember> findByGroupIdAndUserIdForUpdate(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 根据群组ID和角色查询群组成员
     * @param groupId 群组ID
     * @param role 角色
     * @return 群组成员列表
     */
    List<GroupMember> findByGroupIdAndRole(Long groupId, String role);


    /**
     * 根据创建时间范围查询群组成员
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 群组成员列表
     */
    List<GroupMember> findByCreateTimeBetween(Date startDate, Date endDate);

    /**
     * 根据邀请者ID查询群组成员
     * @param inviterId 邀请者ID
     * @return 群组成员列表
     */
    List<GroupMember> findByInviterId(Long inviterId);

    /**
     * 根据群组ID列表批量查询群组成员
     * @param groupIds 群组ID列表
     * @return 群组成员列表
     */
    List<GroupMember> findByGroupIdIn(List<Long> groupIds);

    /**
     * 根据用户ID列表批量查询群组成员
     * @param userIds 用户ID列表
     * @return 群组成员列表
     */
    List<GroupMember> findByUserIdIn(List<Long> userIds);

    /**
     * 判断用户是否在群组中
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 批量删除群组成员
     * @param ids 群组成员ID列表
     */
    void deleteByIdIn(List<Long> ids);

    /**
     * 批量删除指定群组的成员
     * @param groupId 群组ID
     */
    void deleteByGroupId(Long groupId);
}