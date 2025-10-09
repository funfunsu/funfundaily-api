package com.funfun.schedule.service;

import com.funfun.schedule.entity.User;
import java.util.Date;
import java.util.List;

/**
 * UserService接口，定义用户相关的业务逻辑方法
 */
public interface UserService {

    /**
     * 创建用户
     * @param user 用户对象
     * @return 创建后的用户对象
     */
    User createUser(User user);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户对象
     */
    User getUserById(Long id);

    /**
     * 根据openid查询用户
     * @param openid 小程序平台唯一ID
     * @return 用户对象
     */
    User getUserByOpenid(String openid);

    /**
     * 根据手机号查询用户
     * @param phone 用户手机号
     * @return 用户对象
     */
    User getUserByPhone(String phone);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> getAllUsers();

    /**
     * 根据状态查询用户
     * @param status 账号状态
     * @return 用户列表
     */
    List<User> getUsersByStatus(Integer status);

    /**
     * 根据注册时间范围查询用户
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户列表
     */
    List<User> getUsersByRegisterTimeBetween(Date startDate, Date endDate);

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新后的用户对象
     */
    User updateUser(User user);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 批量删除用户
     * @param ids 用户ID列表
     */
    void deleteUsers(List<Long> ids);

    /**
     * 检查用户是否存在
     * @param openid 小程序平台唯一ID
     * @return 是否存在
     */
    boolean existsByOpenid(String openid);

    /**
     * 检查手机号是否已被使用
     * @param phone 用户手机号
     * @return 是否已被使用
     */
    boolean existsByPhone(String phone);

    /**
     * 批量查询用户
     * @param ids 用户ID列表
     * @return 用户列表
     */
    List<User> getUsersByIds(List<Long> ids);
}