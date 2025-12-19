package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InviteMemberShareDTO extends BaseGroupRequest{
    private String role;
    private String label;
}
