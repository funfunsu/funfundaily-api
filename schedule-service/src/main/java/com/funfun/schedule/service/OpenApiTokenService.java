package com.funfun.schedule.service;

/**
 * 开放接口（OpenAPI / MCP）访问令牌解析。
 *
 * <p>令牌配置存于 {@code group_member.open_api_token} 列：每个群组成员一个令牌，
 * 令牌即绑定该成员（groupId+userId）。groupId 用于数据隔离，userId 用于管理/默认归属。
 * 增删改令牌只需更新该列，无需改配置或重启。
 */
public interface OpenApiTokenService {

    /**
     * 解析令牌绑定的群组成员。
     *
     * @param token 去掉 "Bearer " 前缀后的纯令牌
     * @return 绑定的 (groupId, userId)；令牌为空 / 不存在 / 已删除时返回 {@code null}
     */
    OpenApiPrincipal resolvePrincipal(String token);
}
