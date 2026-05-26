-- 增加会员
insert into user_vip(user_id,vip_type, start_time, end_time,created_at,updated_at)
VALUES (1,1,now(),'2025-12-31',now(),now());

-- 维护discovery

UPDATE universal_record
SET content = '[{"id":null,"itemTitle":"任务打卡","itemType":"path","uri":"/pages/tabBar/task","status":"active","category":"打卡与激励","icon":"✅"},{"id":null,"itemTitle":"积分管理","itemType":"path","uri":"/subPackages/points/pages/points-product-manage","status":"active","category":"打卡与激励","icon":"🎯"},{"id":null,"itemTitle":"积分兑换","itemType":"path","uri":"/subPackages/points/pages/points-exchange","status":"active","category":"打卡与激励","icon":"🎁"},{"id":null,"itemTitle":"汉字书写","itemType":"path","uri":"/subPackages/study-tools/pages/writing/stroke-order","status":"active","category":"学习成长","icon":"✍️"},{"id":null,"itemTitle":"理财计划","itemType":"path","uri":"/subPackages/financial-plan/pages/list/index","status":"active","category":"家庭财务","icon":"📈"},{"id":null,"itemTitle":"家庭账本","itemType":"path","uri":"/subPackages/study-tools/pages/ledger/index","status":"active","category":"家庭财务","icon":"📒"},{"id":null,"itemTitle":"邀请函","itemType":"path","uri":"/subPackages/invitation/pages/list","status":"active","category":"生活工具","icon":"✉️"},{"id":null,"itemTitle":"大事记","itemType":"path","uri":"/subPackages/event/pages/event/index","status":"active","category":"记录美好","icon":"✨"}]'
WHERE scene = 'system'
  AND scene_var = 'discovery'
  AND business_key = 'default';


