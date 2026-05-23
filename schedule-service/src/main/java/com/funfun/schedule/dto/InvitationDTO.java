package com.funfun.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 邀请函 DTO（基于 schedule_item 存储）。
 * 既用于接口入参（save / accept），也用于列表 / 详情出参。
 * 时间字段用字符串传输，格式 "yyyy-MM-dd HH:mm"，与前端 DatePicker 输出一致。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationDTO {

    /** schedule_item 主键 */
    private Long id;

    /** 群组 ID */
    private Long groupId;

    /** 创建人（发出邀请的人） */
    private Long createdBy;

    /** 持有人：发出记录=创建人，收到记录=收下邀请的人 */
    private Long userId;

    /** 方向：sent=我发出的，received=我收到的 */
    private String direction;

    /** 收到记录指向的原「发出邀请」id；发出记录为 0 */
    private Long parentId;

    private String title;

    /** 活动开始时间 "yyyy-MM-dd HH:mm" */
    private String startTime;

    /** 活动结束时间 "yyyy-MM-dd HH:mm" */
    private String endTime;

    /** 活动地点 */
    private String address;

    /** 邀请正文 */
    private String body;

    /** 卡片样式 id */
    private String cardStyle;

    /** 落款 */
    private String signature;

    /** 受邀人称呼（收到记录上记录收下时的称呼，可空） */
    private String recipientName;

    /** 变更记录（仅收到记录可能有值；原邀请时间/地点变更后追加） */
    private List<ScheduleItemChange> changes;
}
