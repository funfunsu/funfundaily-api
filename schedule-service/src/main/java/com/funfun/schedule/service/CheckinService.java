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

    /**
     * 戒断事件按天反馈（达成/破戒），写入一条 checkin_record。
     * 与普通打卡不同：按天 upsert（同一天再次反馈覆盖原记录，允许改主意），
     * 反馈结果存 extra.feedback（"persist" 坚持 / "relapse" 破戒），不计积分。
     * @param requestDto 含 groupId/userId/operatorId/taskId/taskTime/extra(feedback)
     * @return 打卡记录ID
     */
    Long performAbstainFeedback(CheckinRecordDTO requestDto) ;
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Long taskId, LocalDate from, LocalDate to) ;
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Set<String> taskKeys) ;
}
