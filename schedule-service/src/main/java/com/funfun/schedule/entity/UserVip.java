package com.funfun.schedule.entity;

import com.funfun.schedule.enums.VipType;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vip", indexes = {
        @Index(name = "idx_end_time", columnList = "end_time")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_id", columnNames = "user_id")
})
@Data
public class UserVip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 对应 MySQL 的 AUTO_INCREMENT
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.ORDINAL) // 或者 EnumType.STRING，根据你的存储偏好
    @Column(name = "vip_type", nullable = false,columnDefinition = "TINYINT")
    private VipType vipType; // 使用枚举

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}