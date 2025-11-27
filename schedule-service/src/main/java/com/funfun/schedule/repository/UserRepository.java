package com.funfun.schedule.repository;

import com.funfun.schedule.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository接口，用于User实体的数据库操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据openid查询用户
     * @param openid 小程序平台唯一ID
     * @return 用户对象（Optional包装）
     */
    Optional<User> findByOpenid(String openid);

    /**
     * 根据unionid查询用户
     * @param unionid 微信/支付宝UnionID
     * @return 用户列表
     */
    List<User> findByUnionid(String unionid);

    /**
     * 根据手机号查询用户
     * @param phone 用户手机号
     * @return 用户对象（Optional包装）
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据状态查询用户
     * @param status 账号状态
     * @return 用户列表
     */
    List<User> findByStatus(Integer status);

    /**
     * 根据用户标签查询用户
     * @param userTag 用户标签
     * @return 用户列表
     */
    List<User> findByUserTag(String userTag);

    /**
     * 根据注册时间范围查询用户
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户列表
     */
    List<User> findByRegisterTimeBetween(Date startDate, Date endDate);

    /**
     * 根据最后登录时间范围查询用户
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户列表
     */
    List<User> findByLastLoginTimeBetween(Date startDate, Date endDate);

    /**
     * 根据邀请者ID查询用户
     * @param inviterId 邀请者ID
     * @return 用户列表
     */
    List<User> findByInviterId(Long inviterId);

    /**
     * 根据删除标志查询用户
     * @param deleteFlag 删除标志
     * @return 用户列表
     */
    List<User> findByDeleteFlag(Integer deleteFlag);

    /**
     * 根据用户ID列表批量查询用户
     * @param ids 用户ID列表
     * @return 用户列表
     */
    List<User> findByIdIn(List<Long> ids);

    /**
     * 根据openid判断用户是否存在
     * @param openid 小程序平台唯一ID
     * @return 是否存在
     */
    boolean existsByOpenid(String openid);

    /**
     * 根据手机号判断用户是否存在
     * @param phone 用户手机号
     * @return 是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 批量删除用户
     * @param ids 用户ID列表
     */
    void deleteByIdIn(List<Long> ids);
}