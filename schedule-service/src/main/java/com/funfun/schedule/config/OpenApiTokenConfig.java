package com.funfun.schedule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 开放接口（OpenAPI / MCP）的访问令牌配置。
 *
 * <p>采用 Bearer Token 鉴权：每个 token 与一个 groupId 绑定，请求只能访问该群组的数据，
 * 以此实现基于 groupId 的数据隔离。令牌通过配置维护（{@code openapi.tokens}），
 * 后续如需也可平滑迁移到数据库表。
 *
 * <pre>
 * openapi:
 *   tokens:
 *     - token: xxxxxxxx
 *       groupId: 1
 *       name: 家庭A
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "openapi")
@Data
public class OpenApiTokenConfig {

    /** 所有已开通的访问令牌。 */
    private List<TokenEntry> tokens = new ArrayList<>();

    /**
     * 根据 token 解析其绑定的 groupId。
     *
     * @param token 去掉 "Bearer " 前缀后的纯 token
     * @return 绑定的 groupId；token 不存在或被禁用时返回 {@code null}
     */
    public Long resolveGroupId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        for (TokenEntry entry : tokens) {
            if (entry.isEnabled() && token.equals(entry.getToken())) {
                return entry.getGroupId();
            }
        }
        return null;
    }

    /** 根据 token 找到完整配置项（用于日志 / 名称展示）。 */
    public TokenEntry resolve(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        for (TokenEntry entry : tokens) {
            if (entry.isEnabled() && token.equals(entry.getToken())) {
                return entry;
            }
        }
        return null;
    }

    @Data
    public static class TokenEntry {
        /** 访问令牌（建议使用足够随机的长字符串）。 */
        private String token;
        /** 该令牌绑定的群组 ID，所有数据访问都被限定在此 groupId 内。 */
        private Long groupId;
        /** 备注名称（可选，便于识别令牌归属）。 */
        private String name;
        /** 是否启用，默认 true；置为 false 可临时吊销而不删除配置。 */
        private boolean enabled = true;
    }
}
