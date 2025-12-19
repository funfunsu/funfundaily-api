package com.funfun.schedule.repository;

import com.funfun.schedule.entity.UserVip;
import com.funfun.schedule.enums.VipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVipRepository extends JpaRepository<UserVip, Long> {

    /**
     * 根据用户ID查找VIP信息。
     * @param userId 用户ID
     * @return Optional包装的UserVip对象，如果不存在则为空
     */
    Optional<UserVip> findByUserId(Long userId);

    /**
     * 删除指定用户的VIP信息。
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    long deleteByUserId(Long userId);

    /**
     * 查找在指定时间之后过期的所有VIP记录。
     * @param referenceTime 参考时间
     * @return 即将过期或已过期的VIP列表
     */
    List<UserVip> findByEndTimeBefore(LocalDateTime referenceTime);

    /**
     * 更新指定用户的VIP类型和有效期。
     * 注意：此方法需要手动编写SQL或使用 @Modifying 和 @Query。
     * @param userId 用户ID
     * @param vipType 新的VIP类型
     * @param startTime 新的有效期开始时间
     * @param endTime 新的有效期结束时间
     * @return 更新影响的行数
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserVip uv SET uv.vipType = :vipType, uv.startTime = :startTime, uv.endTime = :endTime, uv.updatedAt = CURRENT_TIMESTAMP WHERE uv.userId = :userId")
    int updateUserVip(@Param("userId") String userId,
                      @Param("vipType") VipType vipType,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    // --- 其他可能需要的查询方法 ---
    // 例如：根据VIP类型查找用户
    // List<UserVip> findByVipType(UserVip.VipType vipType);

    // 例如：查找在未来N天内过期的VIP
    // @Query("SELECT uv FROM UserVip uv WHERE uv.endTime BETWEEN CURRENT_TIMESTAMP AND :expiryThreshold")
    // List<UserVip> findExpiringSoon(@Param("expiryThreshold") LocalDateTime expiryThreshold);
}