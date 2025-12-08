package com.funfun.schedule.service;

import com.funfun.schedule.dto.CheckinRecordDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinService {
    /**
     * 用户打卡
     * @param requestDto 包含taskId, groupId, userId(模拟), operatorId(模拟)
     * @return 打卡记录ID
     */
    Long performCheckin(CheckinRecordDTO requestDto) ;
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, LocalDateTime from, LocalDateTime to) ;
}
