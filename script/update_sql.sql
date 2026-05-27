
-- ============================================================
-- 放大 schedule_item.item_type：VARCHAR(8) -> VARCHAR(32)
-- 原 VARCHAR(8) 装不下新事项类型 monthlyPlan（11 字符），保存时报
-- "Value too long for column item_type"。留足余量给未来类型。
-- ============================================================
ALTER TABLE schedule_item MODIFY COLUMN item_type VARCHAR(32) NOT NULL COMMENT '事项类型';

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
