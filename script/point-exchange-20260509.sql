-- =============================================================================
-- 迭代：point-exchange（积分兑换）
-- 日期：2026-05-09
--
-- 说明：本迭代功能为「家长定义积分商品 + 成员用积分兑换」。涉及：
--   1) 之前未同步进版本库、但生产已经在用的若干 DDL（transaction_flow 表、
--      schedule_item.update_scope / parent_id / close_status、
--      checkin_record.task_key 与索引）一并补回。
--   2) 本迭代新增表：point_product（兑换商品）。
--      兑换记录不另起表，复用 transaction_flow（FlowType=POINTS, TxnType=EXPENSE）。
--
-- 上线顺序与幂等性：
--   - 已存在 transaction_flow 的环境会被 DROP 重建（生产保留原行的，请评估
--     是否跳过这一段；当前文件旨在让"全新环境"或"开发环境"一次跑完即可）。
--   - 其余 ALTER 语句不带 IF NOT EXISTS（MySQL 5.7 / 8.0 都不支持），重复
--     执行会报错；已部分执行过的环境请按需挑选。
-- =============================================================================



-- =============================================================================
-- 本迭代（point-exchange）新增表
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 4. point_product（积分兑换商品）
--    家长定义、成员可见。删除走逻辑删除（delete_flag）+ 状态字段（status）。
-- -----------------------------------------------------------------------------
drop table if exists point_product;
CREATE TABLE point_product (
    id             BIGINT      AUTO_INCREMENT PRIMARY KEY,
    group_id       BIGINT      NOT NULL                                  COMMENT '所属群组ID',
    name           VARCHAR(64) NOT NULL                                  COMMENT '商品名称',
    description    VARCHAR(128)         NULL                             COMMENT '商品描述',
    required_score INT         NOT NULL                                  COMMENT '兑换所需积分',
    status         VARCHAR(16) NOT NULL                                  COMMENT '状态：ACTIVE / REMOVED',
    created_by     BIGINT      NOT NULL                                  COMMENT '创建人ID',
    updated_by     BIGINT      NOT NULL                                  COMMENT '最后更新人ID',
    delete_flag    TINYINT     NOT NULL DEFAULT 0                        COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_group_status (group_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='积分兑换商品表';


-- 注：兑换记录不再单独建表，直接复用 transaction_flow（FlowType=POINTS,
-- TransactionType=EXPENSE）。商品 id / 商品名 存放在流水的 extra JSON 字段，
-- 时间走 created_at；查询过滤 transaction_type=2 即可拉到所有兑换流水。
