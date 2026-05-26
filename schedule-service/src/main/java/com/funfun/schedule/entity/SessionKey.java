package com.funfun.schedule.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 微信 sessionKey 实体类（映射 t_session_key 表）
 */
@Entity
@Table(name = "t_session_key")
@Data
public class SessionKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键自增

    @Column(name = "wx_id", unique = true, nullable = false, length = 64)
    private String wxId; // 微信小程序Id（唯一）

    @Column(name = "open_id", unique = true, nullable = false, length = 64)
    private String openId; // 微信 openId（唯一）

    @Column(name = "session_key", nullable = false, length = 128)
    private String sessionKey; // 微信 sessionKey

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime; // 过期时间

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime; // 创建时间（自动填充）

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime; // 更新时间（自动填充）
}