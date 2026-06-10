package com.funfun.schedule.repository;

import com.funfun.schedule.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * CheckinRecordRepository接口，用于CheckinRecord实体的数据库操作
 */
@Repository
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    @Query("SELECT c FROM CheckinRecord c WHERE c.taskId = :taskId AND  c.groupId = :groupId AND c.userId = :userId AND c.taskTime >= :from AND c.taskTime <= :to")
    Optional<CheckinRecord> getByUserIdAndTaskIdAndGroupIdAndTaskTimeBetween(
            @Param("userId") Long userId,
            @Param("taskId") Long taskId,
            @Param("groupId") Long groupId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * 范围查询：按 group/user/时间窗
     */
    @Query("SELECT c FROM CheckinRecord c WHERE c.groupId = :groupId AND c.userId = :userId AND c.taskTime >= :from AND c.taskTime < :to ORDER BY c.completeTime DESC")
    List<CheckinRecord> findByGroupIdAndUserIdAndTaskTimeBetween(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * 范围查询 + 单 taskId：按 group/user/单个 taskId/时间窗
     */
    @Query("SELECT c FROM CheckinRecord c WHERE c.groupId = :groupId AND c.userId = :userId AND c.taskId = :taskId AND c.taskTime >= :from AND c.taskTime < :to ORDER BY c.completeTime DESC")
    List<CheckinRecord> findByGroupIdAndUserIdAndTaskTimeBetween(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("taskId") Long taskId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * 按一组 taskKey 拉记录（任务的"当前周期"键，例如 "42:2026-05-09"）。
     */
    List<CheckinRecord> findByGroupIdAndUserIdAndTaskKeyIn(Long groupId, Long userId, Set<String> taskKeys);

    /**
     * 当前周期下该任务已完成几次（用于"打卡 N 次"业务，多次累计）。
     */
    int countByGroupIdAndUserIdAndTaskKey(Long groupId, Long userId, String taskKey);

    /**
     * 取该 group/user/taskKey 下最新一条记录（戒断反馈按天 upsert：同一天再次反馈覆盖原记录）。
     */
    Optional<CheckinRecord> findFirstByGroupIdAndUserIdAndTaskKeyOrderByCompleteTimeDesc(Long groupId, Long userId, String taskKey);
}
