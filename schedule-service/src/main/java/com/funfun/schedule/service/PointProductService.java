package com.funfun.schedule.service;

import com.funfun.schedule.dto.point.CreatePointProductCommand;
import com.funfun.schedule.dto.point.UpdatePointProductCommand;
import com.funfun.schedule.entity.PointProduct;

import java.util.List;

/**
 * 兑换商品领域服务。
 */
public interface PointProductService {

    /**
     * 查询群组内可兑换商品列表。
     */
    List<PointProduct> listActiveProducts(Long groupId, Long requesterUserId);

    /**
     * 创建兑换商品。
     */
    PointProduct createProduct(CreatePointProductCommand command);

    /**
     * 更新兑换商品。
     */
    PointProduct updateProduct(UpdatePointProductCommand command);

    /**
     * 移除兑换商品。
     */
    void removeProduct(Long productId, Long groupId, Long operator);
}
