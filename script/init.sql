CREATE TABLE schedule_item
(
    id               BIGINT AUTO_INCREMENT COMMENT 'ID'
        PRIMARY KEY,
    item_title       VARCHAR(64)  NULL COMMENT '事项标题',
    item_desc        VARCHAR(128) NULL COMMENT '事项描述',
    location         VARCHAR(128) NULL COMMENT '地点',
    repeat_type      VARCHAR(32)  NULL COMMENT '重复类型',
    repeat_keys      VARCHAR(128) NULL COMMENT '重复键值（如周一、每月1号等）',
    repeat_start_day DATE         NULL COMMENT '重复开始日期',
    repeat_end_day   DATE         NULL COMMENT '重复结束日期',
    item_type        VARCHAR(8)   NOT NULL COMMENT '事项类型',
    start_time       DATETIME     NOT NULL COMMENT '开始时间',
    end_time         DATETIME     NOT NULL COMMENT '结束时间', -- 注意：原Java注释为'结束时间'，但字段名是 endTime
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    group_id         BIGINT       NOT NULL COMMENT '组ID',
    -- 新增的列
    extra            TEXT         NULL COMMENT '扩展参数',     -- 使用TEXT类型更适合存储可能较长的JSON或其他格式的扩展数据
    label            VARCHAR(256) NULL COMMENT '标签',
    create_by        BIGINT       NOT NULL COMMENT '创建人ID',
    update_by        BIGINT       NOT NULL COMMENT '最后更新人ID',
    create_time      DATETIME     NOT NULL COMMENT '创建时间',
    update_time      DATETIME     NOT NULL COMMENT '最后更新时间',
    -- 索引
    INDEX schedule_item_idx (group_id, user_id)
) COMMENT ='日程事项表';



CREATE TABLE `user`
(
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    `unionid`         VARCHAR(64)               DEFAULT NULL COMMENT '微信/支付宝UnionID（多端统一标识）',
    `openid`          VARCHAR(64)      NOT NULL COMMENT '小程序平台唯一ID',
    `nickname`        VARCHAR(64)               DEFAULT NULL COMMENT '用户昵称（处理特殊字符）',
    `avatar_url`      VARCHAR(255)              DEFAULT NULL COMMENT '用户头像URL',
    `gender`          TINYINT UNSIGNED          DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `phone`           VARCHAR(20)               DEFAULT NULL COMMENT '用户手机号（加密/脱敏存储）',
    `country`         VARCHAR(32)               DEFAULT NULL COMMENT '国家/地区',
    `province`        VARCHAR(32)               DEFAULT NULL COMMENT '省份',
    `city`            VARCHAR(32)               DEFAULT NULL COMMENT '城市',
    `language`        VARCHAR(16)               DEFAULT 'zh_CN' COMMENT '用户语言（默认中文简体）',
    `register_time`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_login_time` DATETIME                  DEFAULT NULL COMMENT '最后登录时间',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用，1-正常，2-待审核，3-临时封禁',
    `user_tag`        VARCHAR(128)              DEFAULT NULL COMMENT '用户标签（逗号分隔，如“学生,会员”）',
    `ext_info`        JSON                      DEFAULT NULL COMMENT '扩展信息（JSON格式，存储灵活字段）',
    `delete_flag`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    inviter_id        bigint UNSIGNED  null,
    PRIMARY KEY (`id`),
    KEY `idx_openid` (`openid`) COMMENT '避免同一小程序重复创建用户',
    KEY `idx_unionid` (`unionid`) COMMENT '普通索引：快速关联多端账号',
    KEY `idx_register_time` (`register_time`) COMMENT '普通索引：按注册时间查询用户',
    KEY `idx_last_login_time` (`last_login_time`) COMMENT '普通索引：查询活跃用户'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='小程序用户表（兼容微信/支付宝等平台）';

drop table fun_group;
create table fun_group
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    group_name    VARCHAR(64)     null,
    group_desc    VARCHAR(128)    null,
    type          tinyint(4)               default 0,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    creator       bigint UNSIGNED not null,
    PRIMARY KEY (`id`),
    KEY `idx_creator` (`creator`) COMMENT '普通索引：创建者'
);

drop table group_member;
create table group_member
(
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    group_id      bigint UNSIGNED  not null,
    user_id       bigint UNSIGNED  not null,
    role          VARCHAR(16)      null,
    score         bigint           not null default 0,
    `delete_flag` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    inviter_id    bigint UNSIGNED  null,
    removed_id    bigint UNSIGNED  null,
    PRIMARY KEY (`id`),

    index idx_group_id (group_id, user_id)
);



create table checkin_record
(
    `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    task_id         bigint UNSIGNED  not null,
    user_id         bigint UNSIGNED  not null,
    group_id        bigint UNSIGNED  not null,
    `complete_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    extra           TEXT             NULL COMMENT '扩展参数', -- 使用TEXT类型更适合存储可能较长的JSON或其他格式的扩展数据
    `delete_flag`   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_group_user` (`group_id`, `user_id`),
    INDEX `idx_task_id` (`task_id`),                          -- 根据 task_id 查询需求添加
    INDEX `idx_complete_time` (`complete_time`)               -- 根据时间范围查询需求添加
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务打卡记录表';;
create table score_flow
(
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    `flow_type`   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '流水类型：0-入账，1-出账',
    score         int              not null,
    balance  int              not null COMMENT '剩余积分',
    user_id       bigint UNSIGNED  not null,
    group_id      bigint UNSIGNED  not null,
    event_name    varchar(128)     not null,
    label         VARCHAR(256)     NULL COMMENT '标签',
    `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    extra         TEXT             NULL COMMENT '扩展参数', -- 使用TEXT类型更适合存储可能较长的JSON或其他格式的扩展数据
    operator      bigint UNSIGNED  not null,
    `delete_flag` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_group_user` (`group_id`, `user_id`, `create_time`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='积分流水表';


CREATE TABLE `t_session_key`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wx_id`       varchar(64)  NOT NULL COMMENT '小程序ID（唯一标识）',
    `open_id`     varchar(64)  NOT NULL COMMENT '微信用户openId（唯一标识）',
    `session_key` varchar(128) NOT NULL COMMENT '微信code2session返回的sessionKey',
    `expire_time` datetime     NOT NULL COMMENT '过期时间（72小时，与微信一致）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_open_id` (wx_id, `open_id`) COMMENT 'openId唯一索引（避免重复存储）',
    KEY `idx_expire_time` (`expire_time`) COMMENT '过期时间索引（优化清理效率）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='微信sessionKey存储表（集群共享）';



CREATE TABLE `share_record`
(
    `id`         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `token`      VARCHAR(64)     NOT NULL COMMENT '唯一分享令牌（如UUID）',
    `scene_code` VARCHAR(64)     NOT NULL COMMENT '分享场景',
    `content`    TEXT            NOT NULL COMMENT '分享内容（JSON格式字符串）',
    `creator_id` bigint UNSIGNED not null COMMENT '分享者ID（如用户ID）',
    `expires_at` DATETIME        NOT NULL COMMENT '过期时间',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='分享记录表';
