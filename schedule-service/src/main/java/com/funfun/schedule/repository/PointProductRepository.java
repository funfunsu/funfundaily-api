package com.funfun.schedule.repository;

import com.funfun.schedule.entity.PointProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * 兑换商品仓储。
 */
@Repository
public interface PointProductRepository extends JpaRepository<PointProduct, Long> {

    /**
     * 查询群组内指定状态的商品列表。
     */
    List<PointProduct> findByGroupIdAndStatusOrderByIdDesc(Long groupId, String status);

    /**
     * 查询群组内商品。
     */
    Optional<PointProduct> findByIdAndGroupId(Long id, Long groupId);

    /**
     * 兑换流程中加锁读取商品，避免并发下读取到脏状态。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PointProduct p where p.id = :id")
    Optional<PointProduct> findByIdForUpdate(@Param("id") Long id);
}
