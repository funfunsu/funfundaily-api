package com.funfun.schedule.service;

import com.funfun.schedule.dto.point.PointExchangeCommand;
import com.funfun.schedule.dto.point.PointExchangeQuery;
import com.funfun.schedule.dto.point.PointExchangeRecordPageResult;
import com.funfun.schedule.dto.point.PointExchangeResult;

/**
 * 积分兑换领域服务。
 */
public interface PointExchangeService {

    /**
     * 发起积分兑换。
     */
    PointExchangeResult exchange(PointExchangeCommand command);

    /**
     * 查询兑换记录。
     */
    PointExchangeRecordPageResult queryRecords(PointExchangeQuery query);
}
