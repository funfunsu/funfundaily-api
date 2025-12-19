package com.funfun.schedule.service;

import com.funfun.schedule.entity.UserVip;
import com.funfun.schedule.enums.VipType;
import com.funfun.schedule.repository.UserVipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserVipService {

    private static final Logger logger = LoggerFactory.getLogger(UserVipService.class);

    @Autowired
    private UserVipRepository userVipRepository;

    /**
     * 为用户授予或更新VIP资格。
     * 如果用户已有VIP记录，则更新；否则创建新记录。
     *
     * @param userId    用户ID
     * @param vipType   VIP类型
     * @param durationDays VIP持续天数 (用于计算结束时间，对于终身VIP可以传入特殊值或忽略)
     * @return 保存后的UserVip实体
     */
    public UserVip grantOrUpdateVip(Long userId, VipType vipType, long durationDays) {
        if (userId == null ||  vipType == null) {
            throw new IllegalArgumentException("User ID and VIP Type cannot be null or empty");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now; // 默认立即生效
        LocalDateTime endTime;

        if (vipType == VipType.LIFETIME) {
            // 对于终身VIP，设置一个遥远的未来日期
            endTime = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        } else {
            endTime = now.plusDays(durationDays);
        }

        Optional<UserVip> existingVipOpt = userVipRepository.findByUserId(userId);
        UserVip userVip;

        if (existingVipOpt.isPresent()) {
            // 更新现有记录
            userVip = existingVipOpt.get();
            userVip.setVipType(vipType);
            userVip.setStartTime(startTime);
            userVip.setEndTime(endTime);
            // updatedAt 会在 preUpdate 回调中自动更新
            logger.info("Updating VIP for user {}: Type={}, Start={}, End={}", userId, vipType, startTime, endTime);
        } else {
            // 创建新记录
            userVip = new UserVip();
            userVip.setUserId(userId);
            userVip.setVipType(vipType);
            userVip.setStartTime(startTime);
            userVip.setEndTime(endTime);
            // createdAt 和 updatedAt 会在 prePersist 回调中自动更新
            logger.info("Granting new VIP to user {}: Type={}, Start={}, End={}", userId, vipType, startTime, endTime);
        }

        return userVipRepository.save(userVip);
    }

    /**
     * 移除用户的VIP资格。
     *
     * @param userId 用户ID
     * @return 是否成功删除
     */
    public boolean revokeVip(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        long deletedCount = userVipRepository.deleteByUserId(userId);
        boolean success = deletedCount > 0;
        if (success) {
            logger.info("Revoked VIP for user {}", userId);
        } else {
            logger.info("Attempted to revoke VIP for user {}, but no record found.", userId);
        }
        return success;
    }

    /**
     * 获取用户的当前VIP信息。
     *
     * @param userId 用户ID
     * @return Optional包装的UserVip对象
     */
    public Optional<UserVip> getUserVip(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return userVipRepository.findByUserId(userId);
    }

    /**
     * 检查用户当前是否为VIP。
     * 判断依据：存在记录且当前时间在 start_time 和 end_time 之间。
     *
     * @param userId 用户ID
     * @return true 如果是有效VIP，false otherwise
     */
    public boolean isUserVipValid(Long userId) {
        Optional<UserVip> userVipOpt = getUserVip(userId);
        if (userVipOpt.isPresent()) {
            UserVip userVip = userVipOpt.get();
            LocalDateTime now = LocalDateTime.now();
            return !now.isBefore(userVip.getStartTime()) && !now.isAfter(userVip.getEndTime());
        }
        return false; // 没有VIP记录或记录无效
    }

    /**
     * 获取所有已过期的VIP记录。
     * 可用于定时任务清理或通知。
     *
     * @return 已过期的UserVip列表
     */
    public List<UserVip> getExpiredVips() {
        LocalDateTime now = LocalDateTime.now();
        return userVipRepository.findByEndTimeBefore(now);
    }

    /**
     * 手动更新用户的VIP信息（不常用，grantOrUpdateVip更通用）。
     * @param userId 用户ID
     * @param vipType 新VIP类型
     * @param startTime 新开始时间
     * @param endTime 新结束时间
     * @return 更新影响的行数
     */
    public int manuallyUpdateUserVip(String userId, VipType vipType, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || vipType == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("All parameters must be provided");
        }
        return userVipRepository.updateUserVip(userId, vipType, startTime, endTime);
    }
}