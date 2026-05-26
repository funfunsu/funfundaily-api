package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * 开放接口（OpenAPI / MCP）返回的待办任务视图。
 *
 * <p>对内部 {@link com.funfun.schedule.entity.ScheduleItem}（itemType=task）做精简映射，
 * 只暴露调用方真正需要的字段：标题（title）、内容（content）以及完成进度等元信息。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TodoTaskDTO {

    /** 任务 ID。 */
    private Long id;

    /** 任务标题（对应 schedule_item.item_title）。 */
    private String title;

    /** 任务内容 / 描述（对应 schedule_item.item_desc）。 */
    private String content;

    /** 所属群组 ID。 */
    private Long groupId;

    /** 任务归属成员 ID。 */
    private Long userId;

    /** 父任务 ID（无父任务时为 0）。 */
    private Long parentId;

    /** 任务子类型：Habit（按时间）/ Todo（按次数），来自 extra.taskType。 */
    private String taskType;

    /** 重复类型：none/daily/weekly/monthly/yearly 等。 */
    private String repeatType;

    /** 当前周期需要完成的次数（来自 extra.totalCount）。 */
    private Integer totalCount;

    /** 当前周期已完成的次数（按 userId 统计，未指定 userId 时为 null）。 */
    private Integer completedCount;

    /** 当前周期是否已完成（completedCount >= totalCount）。 */
    private Boolean completed;

    /** 创建时间。 */
    private Date createTime;
}
