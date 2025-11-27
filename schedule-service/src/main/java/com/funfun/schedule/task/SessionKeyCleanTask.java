package com.funfun.schedule.task;

import com.funfun.schedule.service.SessionKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时清理过期 sessionKey 任务（生产环境必备）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionKeyCleanTask {

    private final SessionKeyService sessionKeyService;

    /**
     * 定时执行：每小时清理一次过期数据（可根据需求调整频率）
     * cron 表达式：0 0 * * * ? → 整点执行（如 00:00、01:00、...、23:00）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredSessionKey() {
        log.info("开始执行过期 sessionKey 清理任务");
        sessionKeyService.cleanExpired();
        log.info("过期 sessionKey 清理任务执行结束");
    }
}