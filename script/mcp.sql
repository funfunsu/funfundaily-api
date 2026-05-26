
-- ============================================================
-- 开放接口（OpenAPI / MCP）令牌：表配置，存于 group_member.open_api_token
-- 一行 group_member = 一个令牌，绑定到具体成员（同时确定 groupId + userId）。
-- 查询/轮换/吊销按成员 id 管理。
-- ============================================================


-- 1) 加列
ALTER TABLE group_member ADD COLUMN open_api_token VARCHAR(128) NULL;
CREATE INDEX idx_group_member_open_api_token ON group_member (open_api_token);