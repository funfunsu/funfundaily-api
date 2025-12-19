
CREATE TABLE `user_vip` (
                            `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            user_id          BIGINT       NOT NULL COMMENT '用户ID',
                            `vip_type` TINYINT NOT NULL DEFAULT 0 COMMENT 'VIP 类型 (枚举值，例如: 0 - 普通用户, 1 - 月度VIP, 2 - 年度VIP, 3 - 终身VIP)',
                            `start_time` DATETIME NOT NULL COMMENT 'VIP 开始生效时间',
                            `end_time` DATETIME NOT NULL COMMENT 'VIP 结束时间 (对于终身VIP，可以设置一个很远的未来日期，如 9999-12-31 23:59:59)',
                            `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                            `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_user_id` (`user_id`), -- 假设一个用户同一时间只能有一种有效的VIP状态，如果是多条记录则移除此约束
                            KEY `idx_end_time` (`end_time`) -- 为了方便查询即将过期或已过期的VIP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户VIP信息表';