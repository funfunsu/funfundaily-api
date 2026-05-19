-- =============================================================================
-- 理财计划（financial-plan）相关表 DDL
-- =============================================================================
-- 与 JPA 实体保持一致（com.funfun.schedule.entity 包下：
--   FinancialPlan / FinancialPlanAsset / RealizationBatch / RealizationOperation）。
--
-- 设计取舍：脚本只声明字段、主键、查询用索引；外键/取值范围等约束统一交由
-- 应用层（service + DTO 校验）兜底，避免库表层级耦合，方便迁移与回滚。
--
-- 适配 MySQL 8.0+。
-- =============================================================================

-- ------------------------------------------------------------
-- 1) 理财计划主表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_plan (
  plan_id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划主键',
  group_id         BIGINT NOT NULL COMMENT '所属群组/家庭 id',
  owner_user_id    BIGINT NOT NULL COMMENT '负责人用户 id',
  plan_name        VARCHAR(128)    NOT NULL COMMENT '计划名称',
  -- 计划类型字段保留兼容旧 API（SAVINGS|STOCK），新模型默认 SAVINGS，类型实际由各 asset 自行声明。
  plan_type        VARCHAR(16)     NOT NULL COMMENT 'SAVINGS|STOCK（已弱化，类型下移到标的层）',
  stock_sub_type   VARCHAR(16)     NULL     COMMENT 'EQUITY|OPTION（已弃用，保留兼容）',
  status           VARCHAR(16)     NOT NULL COMMENT 'ACTIVE|ARCHIVED',
  time_range_type  VARCHAR(16)     NOT NULL COMMENT 'YEAR|CUSTOM',
  fiscal_year      INT             NULL     COMMENT '财年；YEAR 模式必填',
  start_date       DATE            NOT NULL COMMENT '起始日期',
  end_date         DATE            NOT NULL COMMENT '结束日期；归档时被设为归档当日',
  remark           VARCHAR(1024)   NULL     COMMENT '备注',
  target_profit    DECIMAL(24, 8)  NOT NULL COMMENT '用户在计划层设定的目标盈利（CNY）',
  version          INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删：0-否，1-是',

  PRIMARY KEY (plan_id),
  KEY idx_financial_plan_group_status (group_id, status),
  KEY idx_financial_plan_owner_status (owner_user_id, status),
  KEY idx_financial_plan_fiscal_year  (fiscal_year)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '理财计划主表';


-- ------------------------------------------------------------
-- 2) 理财计划标的（一只股票 + 所属市场 + 目标盈利）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_plan_asset (
  asset_id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '标的主键',
  plan_id       BIGINT NOT NULL COMMENT '所属计划 id',
  stock_name    VARCHAR(128)    NOT NULL COMMENT '股票名称（可读名）',
  market        VARCHAR(8)      NOT NULL COMMENT '市场：US|HK|CN',
  target_profit DECIMAL(24, 8)  NOT NULL COMMENT '用户在该标的上的目标盈利（原币种）',
  sequence_no   INT             NOT NULL DEFAULT 1 COMMENT '排序序号',
  version       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删：0-否，1-是',

  PRIMARY KEY (asset_id),
  KEY idx_financial_plan_asset_plan_id (plan_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '理财计划标的：股票名 + 市场 + 用户目标盈利';


-- ------------------------------------------------------------
-- 3) 兑现批次（标的下的「一次预期买卖」单元）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS realization_batch (
  batch_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次主键',
  plan_id            BIGINT NOT NULL COMMENT '所属计划 id',
  asset_id           BIGINT NOT NULL COMMENT '所属标的 id',
  batch_name         VARCHAR(128)    NULL     COMMENT '批次名称（可选）',
  batch_type         VARCHAR(16)     NOT NULL COMMENT 'EQUITY|DERIVATIVE',
  direction          VARCHAR(16)     NULL     COMMENT 'CALL|PUT|SHORT_CALL|SHORT_PUT，仅 DERIVATIVE 有意义',
  quantity           DECIMAL(24, 8)  NOT NULL COMMENT '计划数量（仅作参考，不限定买卖上限）',
  plan_buy_price     DECIMAL(20, 8)  NOT NULL COMMENT '预期买入价（DERIVATIVE 卖空时可为负）',
  plan_sell_price   DECIMAL(20, 8)  NOT NULL COMMENT '预期卖出价（DERIVATIVE 卖空时可为负）',
  expiration_date    DATE            NULL     COMMENT '到期日；仅 DERIVATIVE 有意义',
  stage_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING_BUY' COMMENT 'PENDING_BUY|PARTIAL_BOUGHT|PENDING_SELL|COMPLETED',
  actual_buy_price   DECIMAL(20, 8)  NULL     COMMENT '加权平均实际买入价',
  actual_sell_price  DECIMAL(20, 8)  NULL     COMMENT '加权平均实际卖出价',
  actual_buy_amount  DECIMAL(24, 8)  NULL     COMMENT '累计买入金额',
  actual_sell_amount DECIMAL(24, 8)  NULL     COMMENT '累计卖出金额',
  -- WAC 法在每次 SELL 后增量结算的已实现盈利；BUY 不动它。
  actual_profit      DECIMAL(24, 8)  NULL     COMMENT '累计已实现盈利（市场原币种）',
  buy_trade_date     DATE            NULL     COMMENT '最近一次买入日期',
  sell_trade_date    DATE            NULL     COMMENT '最近一次卖出日期',
  fee_total          DECIMAL(24, 8)  NOT NULL DEFAULT 0 COMMENT '累计手续费',
  note               VARCHAR(1024)   NULL     COMMENT '备注',
  version            INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删：0-否，1-是',

  PRIMARY KEY (batch_id),
  KEY idx_realization_batch_asset_stage (asset_id, stage_status),
  KEY idx_realization_batch_plan_id     (plan_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '兑现批次：标的下的一次预期买卖单元';


-- ------------------------------------------------------------
-- 4) 兑现操作明细（一个批次下的多次买/卖）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS realization_operation (
  operation_id   BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作主键',
  batch_id       BIGINT NOT NULL COMMENT '所属批次 id',
  operation_type VARCHAR(8)      NOT NULL COMMENT 'BUY|SELL',
  trade_date     DATE            NOT NULL COMMENT '交易日期',
  price          DECIMAL(20, 8)  NOT NULL COMMENT '实际成交价（DERIVATIVE 卖空时可为负）',
  quantity       DECIMAL(24, 8)  NOT NULL COMMENT '成交数量',
  fee            DECIMAL(24, 8)  NOT NULL DEFAULT 0 COMMENT '本次手续费',
  note           VARCHAR(1024)   NULL     COMMENT '备注',
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

  PRIMARY KEY (operation_id),
  KEY idx_realization_operation_batch_type (batch_id, operation_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '兑现操作明细：一个批次下的多次买/卖';
