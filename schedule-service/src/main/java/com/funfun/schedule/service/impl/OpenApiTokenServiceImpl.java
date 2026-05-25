package com.funfun.schedule.service.impl;

import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.service.OpenApiPrincipal;
import com.funfun.schedule.service.OpenApiTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link OpenApiTokenService} 实现：令牌存在 {@code group_member.open_api_token} 列，按令牌查群组成员。
 */
@Service
public class OpenApiTokenServiceImpl implements OpenApiTokenService {

    private final GroupMemberRepository groupMemberRepository;

    public OpenApiTokenServiceImpl(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OpenApiPrincipal resolvePrincipal(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return groupMemberRepository.findByOpenApiToken(token)
                .map(m -> new OpenApiPrincipal(m.getGroupId(), m.getUserId()))
                .orElse(null);
    }
}
