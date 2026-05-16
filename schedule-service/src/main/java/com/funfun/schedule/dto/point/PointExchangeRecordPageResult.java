package com.funfun.schedule.dto.point;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 兑换记录分页结果。
 */
@Data
public class PointExchangeRecordPageResult {
    private long total;
    private List<PointExchangeRecordItem> list = Collections.emptyList();
}
