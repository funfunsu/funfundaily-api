
-- ============================================================
-- 发现页新增「戒断日记」入口（戒断事件列表页）。
-- 发现页配置存于 universal_record(scene='system', scene_var='discovery')。
-- 用 JSON_ARRAY_APPEND 幂等追加（已存在则不重复加），避免覆盖 prod 其它条目。
-- ============================================================
UPDATE universal_record
SET content = JSON_ARRAY_APPEND(
        content, '$',
        CAST('{"id":null,"itemTitle":"戒断日记","itemType":"path","uri":"/subPackages/abstain/pages/list/index","status":"active","category":"打卡与激励","icon":"🚭"}' AS JSON))
WHERE scene = 'system' AND scene_var = 'discovery' AND business_key = 'default'
  AND JSON_SEARCH(content, 'one', '/subPackages/abstain/pages/list/index') IS NULL;

-- ============================================================
-- #771 理财计划优化：批次只挂正股，期权下沉为操作。
-- realization_operation 增加 instrument / option_type / strike_price / expiration_date，
-- 价格/手续费精度统一到 2 位小数。对既有库做幂等迁移。
-- 适配 MySQL 8.0：用 ADD COLUMN IF NOT EXISTS（8.0.29+）；旧版本请手动判断。
-- ============================================================
ALTER TABLE realization_operation
  ADD COLUMN IF NOT EXISTS instrument      VARCHAR(8)     NOT NULL DEFAULT 'STOCK' COMMENT 'STOCK|OPTION' AFTER batch_id,
  ADD COLUMN IF NOT EXISTS option_type     VARCHAR(8)     NULL     COMMENT 'CALL|PUT，仅 OPTION 有效' AFTER operation_type,
  ADD COLUMN IF NOT EXISTS strike_price    DECIMAL(20, 2) NULL     COMMENT '目标价格（行权价），仅 OPTION 有效' AFTER option_type,
  ADD COLUMN IF NOT EXISTS expiration_date DATE           NULL     COMMENT '到期时间，仅 OPTION 有效' AFTER strike_price;

-- 存量正股操作 instrument 兜底为 STOCK（新列默认值已覆盖，这里再幂等保证一次）。
UPDATE realization_operation SET instrument = 'STOCK' WHERE instrument IS NULL OR instrument = '';

-- 期权操作查询索引（与建表脚本一致）。
ALTER TABLE realization_operation
  ADD KEY IF NOT EXISTS idx_realization_operation_batch_instrument (batch_id, instrument);
