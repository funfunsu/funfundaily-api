package com.funfun.schedule.service;
import com.funfun.schedule.entity.SessionKey;
import com.funfun.schedule.repository.SessionKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SessionKey 服务类（封装存储、查询、清理逻辑）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionKeyService {

    private final SessionKeyRepository sessionKeyRepository;
    private static final int EXPIRE_HOURS = 72; // sessionKey 过期时间（72小时，与微信一致）

    /**
     * 存储/更新 sessionKey（用户重新登录时覆盖旧记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(String wxId,String openId, String sessionKey) {
        // 1. 删除旧记录（避免唯一索引冲突）
        sessionKeyRepository.deleteByWxIdAndOpenId(wxId,openId);
        // 2. 计算过期时间（当前时间 + 72小时）
        LocalDateTime expireTime = LocalDateTime.now().plusHours(EXPIRE_HOURS);
        // 3. 存储新记录
        SessionKey sessionKeyEntity = new SessionKey();
        sessionKeyEntity.setWxId(wxId);
        sessionKeyEntity.setOpenId(openId);
        sessionKeyEntity.setSessionKey(sessionKey);
        sessionKeyEntity.setExpireTime(expireTime);
        sessionKeyRepository.save(sessionKeyEntity);
        log.info("sessionKey 存储成功：openId={}, 过期时间={}", openId, expireTime);
    }

    /**
     * 根据 openId 查询有效的 sessionKey（未过期）
     */
    public String getValidSessionKey(String wxId,String openId) {
        return sessionKeyRepository.findByWxIdAndOpenId(wxId,openId)
                .filter(entity -> entity.getExpireTime().isAfter(LocalDateTime.now())) // 过滤过期记录
                .map(SessionKey::getSessionKey)
                .orElseGet(() -> {
                    log.warn("sessionKey 不存在或已过期：openId={}", openId);
                    return null;
                });
    }

    /**
     * 清理所有过期的 sessionKey（定时任务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        int deleteCount = sessionKeyRepository.deleteExpired(now);
        log.info("清理过期 sessionKey 完成，共删除 {} 条记录", deleteCount);
    }
}