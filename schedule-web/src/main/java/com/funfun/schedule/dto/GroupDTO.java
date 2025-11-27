package com.funfun.schedule.dto;

import lombok.Data;

import java.util.Date;

@Data
public class GroupDTO {
    // 所有字段声明在前
    private Long id; // 群组唯一ID

    private String groupName; // 群组标题

    private String groupDesc; // 群组描述

    private int type; // 类型

    private Date createTime; // 创建时间

    private Long creator; // 创建者ID
}
