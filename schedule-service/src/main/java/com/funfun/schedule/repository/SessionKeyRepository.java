package com.funfun.schedule.repository;

import com.funfun.schedule.entity.SessionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SessionKey 数据库操作接口（JPA 自动实现 CRUD）
 */
@Repository
public interface SessionKeyRepository extends JpaRepository<SessionKey, Long> {

    /**
     * 根据 openId 查询 sessionKey 记录（Optional 避免空指针）
     */
    Optional<SessionKey> findByOpenId(String openId);

    /**
     * 根据 openId 删除记录（用户重新登录时覆盖旧 sessionKey）
     */
    @Transactional(rollbackFor = Exception.class)
    void deleteByOpenId(String openId);

    /**
     * 清理过期的 sessionKey（定时任务调用）
     * @param now 当前时间，删除早于 now 的记录
     */
    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("DELETE FROM SessionKey s WHERE s.expireTime < :now")
    int deleteExpired(LocalDateTime now);


    // ✅ 新增：根据 wxId + openId 查询
    Optional<SessionKey> findByWxIdAndOpenId(String wxId, String openId);

    /**
     * 直接执行 DELETE 语句，不先查询实体
     */
    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("DELETE FROM SessionKey s WHERE s.wxId = :wxId AND s.openId = :openId")
    void deleteByWxIdAndOpenId(@Param("wxId") String wxId, @Param("openId") String openId);
}