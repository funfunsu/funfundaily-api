
-- ============================================================
-- 放大 schedule_item.item_desc：VARCHAR(128) -> TEXT
-- ============================================================
ALTER TABLE schedule_item MODIFY COLUMN item_desc TEXT NULL COMMENT '事项描述';

-- ============================================================
-- 发现页新增「月度计划」入口
-- universal_record(scene=system, scene_var=discovery, business_key=default).content 为 JSON 数组。
-- 用 JSON_ARRAY_APPEND 仅追加该条目，不覆盖已有内容；JSON_SEARCH 守卫保证可重复执行不重复追加。
-- ============================================================
UPDATE universal_record
SET content = JSON_ARRAY_APPEND(
        content, '$',
        CAST('{"id":null,"itemTitle":"月度计划","itemType":"path","uri":"/subPackages/monthly-plan/pages/list/index","status":"active","category":"生活工具","icon":"🗓️"}' AS JSON))
WHERE scene = 'system'
  AND scene_var = 'discovery'
  AND business_key = 'default'
  AND JSON_SEARCH(content, 'one', '/subPackages/monthly-plan/pages/list/index') IS NULL;
