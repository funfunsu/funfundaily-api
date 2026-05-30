package com.funfun.schedule.service;

import com.funfun.schedule.dto.UserInfoDTO;
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
    User createUserByNickname(String nickname);

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
    UserInfoDTO updateUserBaseInfo(UserInfoDTO user);

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

    /**
     * 生成或者获取用户
     * @param openId
     * @return
     */
    Long getOrCreateUserIdByOpenId(String openId,Long invitorId);

    /**
     * 把当前登录（微信）用户的 openid 转移到目标占位用户上。
     * 调用方必须当前已登录，且持有真实 openid；目标用户必须 openid 为空（即 bindType=None）。
     * 调用后：占位用户拥有原微信用户的 openid（下次微信登录即解析为该占位账号）；
     * 原微信用户 openid 置空，session 不再代表真人，前端应清 token 重新走 wx_login。
     */
    void bindOpenidToPlaceholder(Long placeholderUserId);

    /**
     * 仅当 targetUserId 的 openid 为空（未绑定微信）时，更新其昵称。
     * 权限由调用方（Controller）通过 @RequiredDataPermission 在 groupId 维度校验。
     */
    UserInfoDTO updateUnboundUserNickname(Long targetUserId, String nickname);
}