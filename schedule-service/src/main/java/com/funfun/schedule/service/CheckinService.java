package com.funfun.schedule.service;

import com.funfun.schedule.dto.CheckinRecordDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface CheckinService {

    /** 用户打卡：写 checkin_record + 触发 transaction_flow（积分入账） */
    Long performCheckin(CheckinRecordDTO requestDto);

    /** 兼容旧调用：当日范围内全部记录 */
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, LocalDateTime from, LocalDateTime to);

    /** 单 task 范围查询（taskId 可空，空时退化为全量范围查） */
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Long taskId, LocalDate fromDate, LocalDate toDate);

    /** 按一组 taskKey（任务的"周期实例"键）查询 */
    List<CheckinRecordDTO> getRecordList(Long groupId, Long userId, Set<String> taskKeys);
}
