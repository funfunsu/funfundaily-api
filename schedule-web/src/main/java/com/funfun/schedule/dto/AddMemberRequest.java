package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AddMemberRequest extends BaseGroupUserRequest{
    private String nickname;
}
