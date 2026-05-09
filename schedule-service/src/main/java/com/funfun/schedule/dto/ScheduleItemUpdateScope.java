package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务运行期快照：当前最近一次完成时间。
 * 由 service 层在返回 ScheduleItemDTO 前从 checkin_record 推出来填进去。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleItemUpdateScope {
    private LocalDateTime lastCompleteTime;
}
