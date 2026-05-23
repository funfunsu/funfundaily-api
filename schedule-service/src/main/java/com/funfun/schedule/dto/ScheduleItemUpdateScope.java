package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务运行期快照：当前最近一次完成时间。
 * 由 service 层在返回 ScheduleItemDTO 前从 checkin_record 推出来填进去。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleItemUpdateScope {
    private LocalDateTime lastCompleteTime;

    /**
     * 变更记录（按时间追加）。
     * 邀请函场景：原「发出的邀请」时间/地点变更时，会向对应「收到的邀请」记录追加一条。
     */
    private List<ScheduleItemChange> changes;
}
