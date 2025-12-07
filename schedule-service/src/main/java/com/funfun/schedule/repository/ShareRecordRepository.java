package com.funfun.schedule.repository;

import com.funfun.schedule.entity.ShareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShareRecordRepository extends JpaRepository<ShareRecord, Long> {

    Optional<ShareRecord> findByToken(String token);

    // 可选：清理过期记录（定时任务用）
    @Query("DELETE FROM ShareRecord s WHERE s.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}
