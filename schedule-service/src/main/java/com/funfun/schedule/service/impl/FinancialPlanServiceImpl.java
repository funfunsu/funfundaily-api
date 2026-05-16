package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.CreateFinancialPlanCommand;
import com.funfun.schedule.dto.FinancialPlanDetailDTO;
import com.funfun.schedule.dto.QueryFinancialPlanCommand;
import com.funfun.schedule.dto.UpdateFinancialPlanCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.entity.RealizationBatch;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.enums.TimeRangeType;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.repository.RealizationBatchRepository;
import com.funfun.schedule.service.FinancialPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 理财计划领域服务实现。
 */
@Service
public class FinancialPlanServiceImpl implements FinancialPlanService {

    private final FinancialPlanRepository financialPlanRepository;
    private final FinancialPlanAssetRepository financialPlanAssetRepository;
    private final RealizationBatchRepository realizationBatchRepository;

    @Autowired
    public FinancialPlanServiceImpl(FinancialPlanRepository financialPlanRepository,
                                    FinancialPlanAssetRepository financialPlanAssetRepository,
                                    RealizationBatchRepository realizationBatchRepository) {
        this.financialPlanRepository = financialPlanRepository;
        this.financialPlanAssetRepository = financialPlanAssetRepository;
        this.realizationBatchRepository = realizationBatchRepository;
    }

    /** 创建理财计划。 */
    @Override
    @Transactional
    public FinancialPlan createPlan(CreateFinancialPlanCommand command) {
        validateCreateCommand(command);

        FinancialPlan plan = new FinancialPlan();
        plan.setGroupId(command.getGroupId());
        plan.setOwnerUserId(command.getOwnerUserId());
        plan.setPlanName(command.getPlanName().trim());
        plan.setPlanType(command.getPlanType());
        plan.setStockSubType(resolveStockSubType(command.getPlanType(), command.getStockSubType()));
        // 新模型：不保留草稿态，新建即生效；过期/归档由前端按 endDate 与 status 判定。
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setRemark(command.getRemark());
        plan.setTargetProfit(command.getTargetProfit() != null ? command.getTargetProfit() : BigDecimal.ZERO);

        // INV-1：根据 timeRangeType 派生或校验起止日期。
        applyTimeRange(plan,
                command.getTimeRangeType(),
                command.getFiscalYear(),
                command.getStartDate(),
                command.getEndDate());

        return financialPlanRepository.save(plan);
    }

    /** 更新理财计划基础信息、时间窗口与状态。 */
    @Override
    @Transactional
    public FinancialPlan updatePlan(Long planId, UpdateFinancialPlanCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("update command is null");
        }
        FinancialPlan plan = loadPlanForWrite(planId);
        ensureVersionMatches(plan, command.getVersion());

        // INV-7：归档计划禁止编辑。仅允许通过 status 字段保持 ARCHIVED 不变；
        // 其余写入（更名、改时间窗口）一律拒绝。
        boolean keepArchived = plan.getStatus() == PlanStatus.ARCHIVED
                && command.getStatus() == PlanStatus.ARCHIVED;
        if (plan.getStatus() == PlanStatus.ARCHIVED && !keepArchived) {
            FinancialPlanError.FP_PLAN_ALREADY_ARCHIVED.throwsError("planId=" + planId);
        }
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            // 已归档且本次未试图修改：直接返回当前状态，避免后续字段被覆盖。
            return plan;
        }

        if (command.getPlanName() != null && !command.getPlanName().trim().isEmpty()) {
            plan.setPlanName(command.getPlanName().trim());
        }
        if (command.getRemark() != null) {
            plan.setRemark(command.getRemark());
        }
        if (command.getStatus() != null) {
            plan.setStatus(command.getStatus());
        }
        if (command.getTargetProfit() != null) {
            plan.setTargetProfit(command.getTargetProfit());
        }
        if (command.getTimeRangeType() != null) {
            applyTimeRange(plan,
                    command.getTimeRangeType(),
                    command.getFiscalYear(),
                    command.getStartDate(),
                    command.getEndDate());
        }

        return saveWithOptimisticLock(plan);
    }

    /**
     * 归档（停用）理财计划。
     *
     * <p>幂等：忽略客户端传入的 version，重复调用不会报错；
     * 同时把 endDate 设为「归档时间」（今天），方便归档后的筛选与统计。
     */
    @Override
    @Transactional
    public FinancialPlan archivePlan(Long planId, Integer version) {
        FinancialPlan plan = loadPlanForWrite(planId);
        plan.setStatus(PlanStatus.ARCHIVED);
        plan.setEndDate(LocalDate.now());
        return saveWithOptimisticLock(plan);
    }

    /** 按主键查询理财计划。 */
    @Override
    @Transactional(readOnly = true)
    public FinancialPlan getPlan(Long planId) {
        if (planId == null) {
            FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId is null");
        }
        return financialPlanRepository.findByPlanIdAndDeletedFalse(planId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId=" + planId);
                    return null;
                });
    }

    /** 按条件分页查询理财计划。 */
    @Override
    @Transactional(readOnly = true)
    public Page<FinancialPlan> queryPlans(QueryFinancialPlanCommand command) {
        if (command == null) {
            FinancialPlanError.FP_QUERY_INVALID.throwsError("command is null");
        }
        int pageNo = command.getPageNo() == null || command.getPageNo() < 1 ? 1 : command.getPageNo();
        int pageSize = command.getPageSize() == null || command.getPageSize() < 1 ? 20 : command.getPageSize();
        if (pageSize > 200) {
            FinancialPlanError.FP_QUERY_INVALID.throwsError("pageSize too large");
        }
        // 模糊关键字归一化
        String keyword = command.getKeyword();
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return financialPlanRepository.queryByConditions(
                command.getGroupId(),
                keyword,
                command.getExecutionStatus(),
                command.getPlanType(),
                command.getTimeRangeType(),
                command.getStartDate(),
                command.getEndDate(),
                pageable
        );
    }

    /** 加载理财计划完整详情（计划本体 + 标的列表 + 兑现批次列表）。 */
    @Override
    @Transactional(readOnly = true)
    public FinancialPlanDetailDTO loadPlanDetail(Long planId) {
        FinancialPlan plan = getPlan(planId);

        List<FinancialPlanAsset> assets =
                financialPlanAssetRepository.findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId);
        List<RealizationBatch> batches =
                realizationBatchRepository.findByPlanIdAndDeletedFalseOrderByCreatedAtDesc(planId);

        FinancialPlanDetailDTO detail = new FinancialPlanDetailDTO();
        detail.setPlan(plan);
        detail.setAssets(assets);
        detail.setRealizationBatches(batches);
        return detail;
    }

    // ===================== 内部工具方法 =====================

    /** 加载计划用于写入路径，未找到时抛 FP_PLAN_NOT_FOUND。 */
    private FinancialPlan loadPlanForWrite(Long planId) {
        if (planId == null) {
            FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId is null");
        }
        return financialPlanRepository.findByPlanIdAndDeletedFalse(planId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId=" + planId);
                    return null;
                });
    }

    /** 校验客户端传入版本号与数据库当前版本号一致。 */
    private void ensureVersionMatches(FinancialPlan plan, Integer expectedVersion) {
        if (expectedVersion == null) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("missing version");
        }
        if (!Objects.equals(plan.getVersion(), expectedVersion)) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                    "expected=" + expectedVersion + ", actual=" + plan.getVersion());
        }
    }

    /** 校验创建命令的必填字段与组合约束。 */
    private void validateCreateCommand(CreateFinancialPlanCommand command) {
        if (command == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("command is null");
        }
        if (command.getGroupId() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("groupId is required");
        }
        if (command.getOwnerUserId() == null) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("ownerUserId is required");
        }
        if (command.getPlanName() == null || command.getPlanName().trim().isEmpty()) {
            FinancialPlanError.FP_VALIDATION_FAILED.throwsError("planName is required");
        }
        if (command.getPlanType() == null) {
            FinancialPlanError.FP_TYPE_UNSUPPORTED.throwsError("planType is required");
        }
        if (command.getTimeRangeType() == null) {
            FinancialPlanError.FP_WINDOW_INVALID.throwsError("timeRangeType is required");
        }
    }

    /**
     * 计算并写入计划起止日期，遵循 INV-1：
     * <ul>
     *   <li>YEAR 模式：必须提供 fiscalYear，由系统派生该自然年起止时间。</li>
     *   <li>CUSTOM 模式：必须提供 startDate / endDate，且 startDate &lt;= endDate。</li>
     * </ul>
     */
    private void applyTimeRange(FinancialPlan plan,
                                TimeRangeType timeRangeType,
                                Integer fiscalYear,
                                LocalDate startDate,
                                LocalDate endDate) {
        if (timeRangeType == null) {
            FinancialPlanError.FP_WINDOW_INVALID.throwsError("timeRangeType is required");
        }
        plan.setTimeRangeType(timeRangeType);

        if (timeRangeType == TimeRangeType.YEAR) {
            if (fiscalYear == null) {
                FinancialPlanError.FP_WINDOW_INVALID.throwsError("fiscalYear is required for YEAR mode");
            }
            plan.setFiscalYear(fiscalYear);
            plan.setStartDate(LocalDate.of(fiscalYear, 1, 1));
            plan.setEndDate(LocalDate.of(fiscalYear, 12, 31));
        } else {
            // CUSTOM 模式
            if (startDate == null || endDate == null) {
                FinancialPlanError.FP_WINDOW_INVALID.throwsError(
                        "startDate / endDate is required for CUSTOM mode");
            }
            if (startDate.isAfter(endDate)) {
                FinancialPlanError.FP_WINDOW_INVALID.throwsError(
                        "startDate must not be after endDate");
            }
            plan.setFiscalYear(null);
            plan.setStartDate(startDate);
            plan.setEndDate(endDate);
        }
    }

    /** 校验股票子类型与计划类型组合。 */
    private StockSubType resolveStockSubType(PlanType planType, StockSubType stockSubType) {
        if (planType == PlanType.STOCK) {
            if (stockSubType == null) {
                FinancialPlanError.FP_TYPE_UNSUPPORTED.throwsError(
                        "stockSubType is required for STOCK plan");
            }
            return stockSubType;
        }
        // 储蓄类计划无股票子类型。
        return null;
    }

    /** 统一封装乐观锁冲突。 */
    private FinancialPlan saveWithOptimisticLock(FinancialPlan plan) {
        try {
            return financialPlanRepository.save(plan);
        } catch (OptimisticLockingFailureException ex) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("planId=" + plan.getPlanId());
            return null;
        }
    }
}
