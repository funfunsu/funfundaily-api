package com.funfun.schedule.service;

import com.funfun.schedule.dto.CreateFinancialPlanCommand;
import com.funfun.schedule.dto.FinancialPlanDetailDTO;
import com.funfun.schedule.dto.QueryFinancialPlanCommand;
import com.funfun.schedule.dto.UpdateFinancialPlanCommand;
import com.funfun.schedule.entity.FinancialPlan;
import org.springframework.data.domain.Page;

/**
 * 理财计划领域服务。
 *
 * <p>承载创建、更新、归档计划与计划详情聚合查询逻辑；
 * 严格遵循 INV-1（时间窗口）、INV-7（归档不可编辑）等不变量。
 */
public interface FinancialPlanService {

    /**
     * 创建理财计划。
     *
     * @param command 创建命令
     * @return 持久化后的计划实体
     */
    FinancialPlan createPlan(CreateFinancialPlanCommand command);

    /**
     * 更新理财计划基础信息、时间窗口与状态。
     *
     * @param planId  计划主键
     * @param command 更新命令
     * @return 更新后的计划实体
     */
    FinancialPlan updatePlan(Long planId, UpdateFinancialPlanCommand command);

    /**
     * 归档（停用）理财计划。
     *
     * @param planId  计划主键
     * @param version 乐观锁版本号
     * @return 归档后的计划实体
     */
    FinancialPlan archivePlan(Long planId, Integer version);

    /**
     * 按主键查询理财计划，未找到时抛 FP_PLAN_NOT_FOUND。
     *
     * @param planId 计划主键
     * @return 计划实体
     */
    FinancialPlan getPlan(Long planId);

    /**
     * 加载理财计划完整详情（计划本体 + 标的列表 + 兑现批次列表）。
     *
     * @param planId 计划主键
     * @return 详情聚合
     */
    FinancialPlanDetailDTO loadPlanDetail(Long planId);

    /**
     * 按条件分页查询理财计划（对应 API-1）。
     *
     * @param command 查询命令（含 groupId、关键字、状态、分页等）
     * @return 分页结果
     */
    Page<FinancialPlan> queryPlans(QueryFinancialPlanCommand command);
}
