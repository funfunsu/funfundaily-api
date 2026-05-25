package com.funfun.schedule.service;

/**
 * 开放接口（OpenAPI / MCP）访问令牌解析结果：一个 group_member 行绑定一个令牌，
 * 因此令牌可同时唯一确定 groupId（数据隔离边界）与 userId（管理维度，可用于审计/默认归属）。
 */
public record OpenApiPrincipal(Long groupId, Long userId) {
}
