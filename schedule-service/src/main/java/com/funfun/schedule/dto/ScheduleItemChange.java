package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日程项的一条变更记录。
 * 目前用于邀请函：当「发出的邀请」时间/地点变更时，向每条「收到的邀请」记录的 updateScope 追加一条。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleItemChange {
    /** 变更发生时间 */
    private LocalDateTime changeTime;
    /** 人类可读的变更摘要，如「时间调整为 2026-06-01 18:00~20:00；地点调整为 XX」 */
    private String summary;
    /** 变更后的开始时间（冗余存一份，便于前端直接展示） */
    private LocalDateTime startTime;
    /** 变更后的结束时间 */
    private LocalDateTime endTime;
    /** 变更后的地点 */
    private String location;
}
