package com.funfun.schedule.dto;

import lombok.Data;

@Data
public class DiscoveryItemDTO {
    String id;
    String itemTitle;
    String itemType;
    String uri;
    String status;
    /** 分类标识，用于「更多」页按分类分组展示，如 积分中心 / 家庭财务 / 学习成长 / 生活工具。 */
    String category;
    /** 展示图标（emoji），前端缺省时回退到标题首字。 */
    String icon;
}
