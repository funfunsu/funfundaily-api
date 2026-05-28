
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
