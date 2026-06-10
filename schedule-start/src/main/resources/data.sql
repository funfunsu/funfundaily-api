-- 开发环境种子数据（H2 内存库专用）。
-- 由 application-dev.yml 的 spring.sql.init.mode=always 自动加载，
-- 在 Hibernate 建完表（ddl-auto=create-drop）之后执行。
--
-- openid 与 frontend/src/utils/auth.ts 的 dev autoLogin 对齐：
--   const data = { 'code': DateUtils.getDayStartTimeStr(new Date(2026,1,9)) }
--   = "2026-02-09T00:00:00"
-- 后端 WeChatMiniMockServiceImpl 把 code 拼成 "mock-openid-{code}"，
-- 所以登录会按下面这个 openid 查找；data.sql 先把 user 行预插入，
-- 登录时 getOrCreateUserIdByOpenId 直接命中已有 user_id=1，与 group_member 对齐。
-- 如果想换登录用户，改 auth.ts 的 code 或在 H2 控制台手动调整。

-- 预置主测试用户（id=1，与 dev autoLogin 的 openid 严格对齐）
INSERT INTO user (id, openid, nickname, language, register_time, status, delete_flag)
VALUES (1, 'mock-openid-2026-02-09T00:00:00', '测试用户', 'zh_CN', CURRENT_TIMESTAMP, 1, 0);

-- 一个测试群组
INSERT INTO fun_group (id, group_name, group_desc, type, create_time, creator)
VALUES (1, '测试群组', '本地调试用', 0, CURRENT_TIMESTAMP, 1);

-- 把测试用户挂为该群 Admin（同时通过 DataPermissionAspect 与
-- point-exchange 的 isManagerRole 校验，覆盖最广），给 500 积分够多次兑换。
-- 注意：GroupRole 枚举有 Creator/Admin/Member 三个值，区分大小写；
-- point-exchange 的角色判断用 ADMIN/OWNER（大小写不敏感）。'Admin' 能通过两边。
-- open_api_token：开放接口/MCP 的访问令牌，dev 固定，绑定到本成员行（group=1, user=1）。
INSERT INTO group_member (id, group_id, user_id, role, score, delete_flag, create_time, update_time, open_api_token)
VALUES (1, 1, 1, 'Admin', 500, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'dev-openapi-token-group1');

-- 三个 ACTIVE 商品覆盖不同积分档位，方便调试余额校验/边界
INSERT INTO point_product (id, group_id, name, description, required_score, status, created_by, updated_by, delete_flag, create_time, update_time)
VALUES
  (1, 1, '文具礼包', '铅笔+橡皮+笔记本',  50, 'ACTIVE', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, '学习贴纸', '一套精美贴纸',       30, 'ACTIVE', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 1, '拼装玩具', '小型积木玩具',      200, 'ACTIVE', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 给测试用户预置一笔 INCOME 流水（500 积分），让 TransactionFlowService.getCurrentBalance
-- 立即返 500。POINTS=0（FlowType ORDINAL）/ INCOME=1。
-- 注意：必须显式设置 created_at（且明显早于运行时插入的行），否则 Hibernate
-- 用 ORDER BY created_at DESC 取"最近一条"时，因为 NULL 比较结果不确定，
-- 可能拿到这条种子行而不是新写入的流水，导致前端余额永远显示 500。
INSERT INTO transaction_flow (group_id, user_id, transaction_type, flow_type, amount, balance, description, operator, created_at, updated_at)
VALUES (1, 1, 1, 0, 500, 500, '初始测试积分', 1, '2020-01-01 00:00:00', '2020-01-01 00:00:00');


-- 注意：universal_record.content 是 JSON 列，H2 在 MySQL 模式下也会建 JSON 类型；
-- 直接用字符串字面量会被当成"JSON 字符串值"二次编码（取出来多一层引号 + 反斜杠），
-- DiscoveryController 里 fastjson2.parseArray 就会报 "illegal input, offset 1, char \""。
-- 用 H2 的 JSON 字面量 `JSON '...'` 让 H2 当成 JSON 数组存。
INSERT INTO universal_record (scene, scene_var, business_key, content, created_by, updated_by)
VALUES (
  'system',
  'discovery',
  'default',
  JSON '[{"id":null,"itemTitle":"任务打卡","itemType":"path","uri":"/pages/tabBar/task","status":"active","category":"打卡与激励","icon":"✅"},{"id":null,"itemTitle":"积分管理","itemType":"path","uri":"/subPackages/points/pages/points-product-manage","status":"active","category":"打卡与激励","icon":"🎯"},{"id":null,"itemTitle":"积分兑换","itemType":"path","uri":"/subPackages/points/pages/points-exchange","status":"active","category":"打卡与激励","icon":"🎁"},{"id":null,"itemTitle":"汉字书写","itemType":"path","uri":"/subPackages/study-tools/pages/writing/stroke-order","status":"active","category":"学习成长","icon":"✍️"},{"id":null,"itemTitle":"理财计划","itemType":"path","uri":"/subPackages/financial-plan/pages/list/index","status":"active","category":"家庭财务","icon":"📈"},{"id":null,"itemTitle":"家庭账本","itemType":"path","uri":"/subPackages/study-tools/pages/ledger/index","status":"active","category":"家庭财务","icon":"📒"},{"id":null,"itemTitle":"邀请函","itemType":"path","uri":"/subPackages/invitation/pages/list","status":"active","category":"生活工具","icon":"✉️"},{"id":null,"itemTitle":"大事记","itemType":"path","uri":"/subPackages/event/pages/event/index","status":"active","category":"记录美好","icon":"✨"},{"id":null,"itemTitle":"月度计划","itemType":"path","uri":"/subPackages/monthly-plan/pages/list/index","status":"active","category":"生活工具","icon":"🗓️"},{"id":null,"itemTitle":"戒断日记","itemType":"path","uri":"/subPackages/abstain/pages/list/index","status":"active","category":"打卡与激励","icon":"🚭"}]',
  0,
  0
);
