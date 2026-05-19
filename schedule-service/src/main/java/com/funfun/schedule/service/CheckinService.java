package com.funfun.schedule.service;

import com.funfun.schedule.dto.CheckinRecordDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface CheckinService {
    /**
     * 用户打卡
     * @param requestDto 包含taskId, groupId, userId(模拟), operatorId(模拟)
     * @return 打卡记录ID
     */
    Long performCheckin(CheckinRecordDTO requestDto) ;
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Long taskId, LocalDate from, LocalDate to) ;
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Set<String> taskKeys) ;
}
