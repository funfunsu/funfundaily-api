package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 按 taskKey 列表查询 checkin 记录。
 * taskKey 形如 "${taskId}:${periodKey}"（与 ScheduleItemService.getTaskKey 对齐）。
 * 服务端会从中解析出 taskIds 进行查询。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetCheckinRequest extends BaseGroupUserRequest {
    private List<String> taskKeys;
}
