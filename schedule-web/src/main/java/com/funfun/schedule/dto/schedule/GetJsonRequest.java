package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.BaseGroupUserRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetJsonRequest extends BaseGroupUserRequest {
    private String key;
}
