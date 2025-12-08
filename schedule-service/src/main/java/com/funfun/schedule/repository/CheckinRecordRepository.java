package com.funfun.schedule.repository;

import com.funfun.schedule.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * CheckinRecordRepository接口，用于CheckinRecord实体的数据库操作
 */
@Repository
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    Optional<CheckinRecord> findByUserIdAndTaskIdAndGroupId(Long userId, Long taskId, Long groupId);

    /**
     * 根据 groupId, userId 和时间范围查询打卡记录
     * 使用 JPQL (Java Persistence Query Language)
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param from 开始时间 (inclusive)
     * @param to 结束时间 (exclusive)
     * @return 符合条件的打卡记录列表
     */
    @Query("SELECT c FROM CheckinRecord c WHERE c.groupId = :groupId AND c.userId = :userId AND c.completeTime >= :from AND c.completeTime < :to ORDER BY c.completeTime DESC")
    List<CheckinRecord> findByGroupIdAndUserIdAndCompleteTimeBetween(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from, // 使用 LocalDateTime
            @Param("to") LocalDateTime to      // 使用 LocalDateTime
    );

}