package com.funfun.schedule.service;

import com.funfun.schedule.dto.InvitationDTO;

import java.util.List;

/**
 * 邀请函业务（基于 schedule_item 存储）。
 * 发出的邀请 = item_type=invSent；收到的邀请 = item_type=invRecv，parentId 指向对应 invSent 记录。
 */
public interface InvitationService {

    /**
     * 邀请函主页列表：当前用户「我发出的」（限定群组）+「我收到的」（不限群组）。
     * 每条带 direction=sent/received。
     */
    List<InvitationDTO> listForHome(Long userId, Long groupId);

    /**
     * 按 id 查询单条邀请函（发出记录）。
     */
    InvitationDTO getById(Long id);

    /**
     * 创建 / 更新「发出的邀请」。
     * 更新时若 startTime/endTime/address 变化，会级联更新所有「收到的邀请」子记录并追加变更记录。
     */
    InvitationDTO saveSent(Long userId, InvitationDTO dto);

    /**
     * 删除「发出的邀请」（仅创建人）。收到记录保留。
     */
    void deleteSent(Long userId, Long id);

    /**
     * 受邀人「收下邀请」：依据原邀请 id 拷贝当前数据生成一条「收到的邀请」。
     * 幂等：同一用户对同一原邀请重复收下返回已存在记录。
     */
    InvitationDTO accept(Long userId, Long invitationId, String recipientName);
}
