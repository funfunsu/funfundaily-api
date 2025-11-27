package com.funfun.schedule.dto;

import lombok.Data;

import java.util.Date;

/**
 * 群组DTO（数据传输对象）
 * 用于前端与后端之间的数据交互
 */
@Data
public class GroupDTO {

    /**
     * 群组ID
     */
    private Long id;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 类型
     */
    private int  type;

    /**
     * 群组描述
     */
    private String groupDesc;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者ID
     */
    private Long creator;
}