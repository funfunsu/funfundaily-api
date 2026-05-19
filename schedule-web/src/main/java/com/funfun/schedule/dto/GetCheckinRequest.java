package com.funfun.schedule.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetCheckinRequest extends BaseGroupUserRequest{
    private Set<String> taskKeys;
}
