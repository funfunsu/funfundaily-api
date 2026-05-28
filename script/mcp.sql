
-- ============================================================
-- 开放接口（OpenAPI / MCP）令牌：表配置，存于 group_member.open_api_token
-- 一行 group_member = 一个令牌，绑定到具体成员（同时确定 groupId + userId）。
-- 查询/轮换/吊销按成员 id 管理。
-- ============================================================


-- 1) 加列
ALTER TABLE group_member ADD COLUMN open_api_token VARCHAR(128) NULL;
CREATE INDEX idx_group_member_open_api_token ON group_member (open_api_token);


-- ============================================================
-- 放大 schedule_item.item_desc：VARCHAR(128) -> TEXT
-- ============================================================
ALTER TABLE schedule_item MODIFY COLUMN item_desc TEXT NULL COMMENT '事项描述';

-- ============================================================
-- 放大 schedule_item.item_type：VARCHAR(8) -> VARCHAR(32)
-- 原 VARCHAR(8) 装不下新事项类型 monthlyPlan（11 字符），保存时报
-- "Value too long for column item_type"。留足余量给未来类型。
-- ============================================================
ALTER TABLE schedule_item MODIFY COLUMN item_type VARCHAR(32) NOT NULL COMMENT '事项类型';

