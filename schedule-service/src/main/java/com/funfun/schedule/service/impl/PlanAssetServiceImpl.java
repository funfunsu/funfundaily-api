package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.SaveFinancialPlanAssetItem;
import com.funfun.schedule.dto.UpdateFinancialPlanAssetParamsCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.enums.PlanType;
import com.funfun.schedule.enums.StockSubType;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.service.PlanAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 计划标的领域服务实现。
 */
@Service
public class PlanAssetServiceImpl implements PlanAssetService {

    private final FinancialPlanRepository financialPlanRepository;
    private final FinancialPlanAssetRepository financialPlanAssetRepository;

    @Autowired
    public PlanAssetServiceImpl(FinancialPlanRepository financialPlanRepository,
                                FinancialPlanAssetRepository financialPlanAssetRepository) {
        this.financialPlanRepository = financialPlanRepository;
        this.financialPlanAssetRepository = financialPlanAssetRepository;
    }

    /** 批量保存计划标的（新增 + 更新）。 */
    @Override
    @Transactional
    public List<FinancialPlanAsset> saveAssets(Long planId, List<SaveFinancialPlanAssetItem> items) {
        FinancialPlan plan = loadActivePlan(planId);

        if (items == null || items.isEmpty()) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("items is empty");
        }

        // 请求体内自校验：同一批次中 (assetCode, assetType) 不可重复。
        Set<String> uniqueKeys = new HashSet<>();
        for (SaveFinancialPlanAssetItem item : items) {
            validateAssetItem(plan, item);
            String key = buildUniqueKey(item.getAssetCode(), item.getAssetType());
            if (!uniqueKeys.add(key)) {
                FinancialPlanError.FP_ASSET_DUPLICATED.throwsError(key);
            }
        }

        List<FinancialPlanAsset> saved = new ArrayList<>(items.size());
        for (SaveFinancialPlanAssetItem item : items) {
            FinancialPlanAsset entity = upsertAsset(plan, item);
            try {
                saved.add(financialPlanAssetRepository.save(entity));
            } catch (OptimisticLockingFailureException ex) {
                FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                        "assetId=" + entity.getAssetId());
            }
        }

        // 返回保存后的标的列表（按 sequenceNo 升序），保持与查询接口一致。
        return financialPlanAssetRepository
                .findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId);
    }

    /** 调整单个标的的计划参数（价格、数量）。 */
    @Override
    @Transactional
    public FinancialPlanAsset updateAssetParams(Long planId,
                                                Long assetId,
                                                UpdateFinancialPlanAssetParamsCommand command) {
        if (command == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("command is null");
        }
        FinancialPlan plan = loadActivePlan(planId);
        FinancialPlanAsset asset = loadAssetForWrite(assetId);
        ensureAssetBelongsToPlan(asset, plan.getPlanId());
        ensureVersionMatches(asset, command.getVersion());

        // INV-2：价格按 OPTION 规则放行负值；数量必须为正、不得小于已兑现数量。
        validatePlanPrice("planBuyPrice", command.getPlanBuyPrice(), asset.getAssetType(), asset.getStockSubType());
        validatePlanPrice("planSellPrice", command.getPlanSellPrice(), asset.getAssetType(), asset.getStockSubType());
        validatePositiveAmount("planQuantity", command.getPlanQuantity());
        if (command.getPlanQuantity().compareTo(asset.getRealizedQuantity()) < 0) {
            FinancialPlanError.FP_ASSET_QTY_LT_REALIZED.throwsError(
                    "assetId=" + assetId
                            + ", planQuantity=" + command.getPlanQuantity()
                            + ", realizedQuantity=" + asset.getRealizedQuantity());
        }

        asset.setPlanBuyPrice(command.getPlanBuyPrice());
        asset.setPlanSellPrice(command.getPlanSellPrice());
        asset.setPlanQuantity(command.getPlanQuantity());

        try {
            return financialPlanAssetRepository.save(asset);
        } catch (OptimisticLockingFailureException ex) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("assetId=" + assetId);
            return null;
        }
    }

    /** 查询计划下全部有效标的。 */
    @Override
    @Transactional(readOnly = true)
    public List<FinancialPlanAsset> listAssetsByPlanId(Long planId) {
        if (planId == null) {
            FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId is null");
        }
        return financialPlanAssetRepository
                .findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId);
    }

    /** 按主键查询单个有效标的。 */
    @Override
    @Transactional(readOnly = true)
    public FinancialPlanAsset getAsset(Long assetId) {
        return loadAssetForWrite(assetId);
    }

    // ===================== 内部工具方法 =====================

    /** 加载计划并校验是否仍可编辑（未归档），违反 INV-7 时抛 FP_PLAN_ALREADY_ARCHIVED。 */
    private FinancialPlan loadActivePlan(Long planId) {
        if (planId == null) {
            FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId is null");
        }
        FinancialPlan plan = financialPlanRepository
                .findByPlanIdAndDeletedFalse(planId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_PLAN_NOT_FOUND.throwsError("planId=" + planId);
                    return null;
                });
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            FinancialPlanError.FP_PLAN_ALREADY_ARCHIVED.throwsError("planId=" + planId);
        }
        return plan;
    }

    /** 加载标的用于写入路径，未找到时抛 FP_ASSET_NOT_FOUND。 */
    private FinancialPlanAsset loadAssetForWrite(Long assetId) {
        if (assetId == null) {
            FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError("assetId is null");
        }
        return financialPlanAssetRepository
                .findByAssetIdAndDeletedFalse(assetId)
                .orElseGet(() -> {
                    FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError("assetId=" + assetId);
                    return null;
                });
    }

    /** 校验标的所属计划与传入 planId 一致。 */
    private void ensureAssetBelongsToPlan(FinancialPlanAsset asset, Long planId) {
        if (!Objects.equals(asset.getPlanId(), planId)) {
            FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError(
                    "assetId=" + asset.getAssetId() + ", planId=" + planId);
        }
    }

    /** 校验客户端版本号与数据库当前版本号一致。 */
    private void ensureVersionMatches(FinancialPlanAsset asset, Integer expectedVersion) {
        if (expectedVersion == null) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("missing version");
        }
        if (!Objects.equals(asset.getVersion(), expectedVersion)) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                    "expected=" + expectedVersion + ", actual=" + asset.getVersion());
        }
    }

    /** 校验单个标的入参合法性（INV-2 中“金额数量为正”部分 + 类型组合）。 */
    private void validateAssetItem(FinancialPlan plan, SaveFinancialPlanAssetItem item) {
        if (item == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("item is null");
        }
        if (item.getAssetType() == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("assetType is required");
        }
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("assetCode is required");
        }
        if (item.getAssetName() == null || item.getAssetName().trim().isEmpty()) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("assetName is required");
        }
        if (item.getCurrency() == null || item.getCurrency().trim().isEmpty()) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("currency is required");
        }
        if (item.getSequenceNo() == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("sequenceNo is required");
        }
        // 价格：OPTION 允许负数（卖空收取权利金时买入/卖出价可能为负）；其他类型必须正数。
        validatePlanPrice("planBuyPrice", item.getPlanBuyPrice(), item.getAssetType(), item.getStockSubType());
        validatePlanPrice("planSellPrice", item.getPlanSellPrice(), item.getAssetType(), item.getStockSubType());
        validatePositiveAmount("planQuantity", item.getPlanQuantity());

        // 标的类型不再需要与计划类型一致：理财计划不再承载类型语义，由各标的自行声明。
        if (item.getAssetType() == PlanType.STOCK) {
            StockSubType subType = item.getStockSubType() != null
                    ? item.getStockSubType() : plan.getStockSubType();
            if (subType == null) {
                FinancialPlanError.FP_ASSET_INVALID.throwsError("stockSubType is required");
            }
        }
    }

    /** 数值必须严格大于 0。 */
    private void validatePositiveAmount(String fieldName, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError(fieldName + " must be positive");
        }
    }

    /**
     * 校验计划价格：
     * <ul>
     *   <li>不得为 null 或 0；</li>
     *   <li>STOCK + OPTION 允许负数（卖空场景买入/卖出权利金为负）；</li>
     *   <li>其余组合必须为正数。</li>
     * </ul>
     */
    private void validatePlanPrice(String fieldName, BigDecimal price, PlanType assetType, StockSubType subType) {
        if (price == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError(fieldName + " is required");
        }
        if (price.compareTo(BigDecimal.ZERO) == 0) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError(fieldName + " must not be zero");
        }
        boolean allowsNegative = assetType == PlanType.STOCK && subType == StockSubType.OPTION;
        if (!allowsNegative && price.compareTo(BigDecimal.ZERO) < 0) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError(fieldName + " must be positive");
        }
    }

    /**
     * 新增或更新单个标的：
     * <ul>
     *   <li>当 assetId 存在：按主键加载并校验 planId、INV-2 后更新字段。</li>
     *   <li>当 assetId 为空：先按 (planId, assetCode, assetType) 检测重复，再新增实体。</li>
     * </ul>
     */
    private FinancialPlanAsset upsertAsset(FinancialPlan plan, SaveFinancialPlanAssetItem item) {
        if (item.getAssetId() != null) {
            FinancialPlanAsset existing = loadAssetForWrite(item.getAssetId());
            ensureAssetBelongsToPlan(existing, plan.getPlanId());

            // 更新前再次执行 INV-2：planQuantity 不得小于 realizedQuantity。
            if (item.getPlanQuantity().compareTo(existing.getRealizedQuantity()) < 0) {
                FinancialPlanError.FP_ASSET_QTY_LT_REALIZED.throwsError(
                        "assetId=" + existing.getAssetId()
                                + ", planQuantity=" + item.getPlanQuantity()
                                + ", realizedQuantity=" + existing.getRealizedQuantity());
            }

            existing.setAssetType(item.getAssetType());
            existing.setAssetCode(item.getAssetCode().trim());
            existing.setAssetName(item.getAssetName().trim());
            existing.setStockSubType(resolveStockSubType(item, plan));
            existing.setPlanBuyPrice(item.getPlanBuyPrice());
            existing.setPlanSellPrice(item.getPlanSellPrice());
            existing.setPlanQuantity(item.getPlanQuantity());
            existing.setCurrency(item.getCurrency().trim());
            existing.setSequenceNo(item.getSequenceNo());
            return existing;
        }

        // 新增前检查 (planId, assetCode, assetType) 是否重复。
        financialPlanAssetRepository
                .findByPlanIdAndAssetCodeAndAssetTypeAndDeletedFalse(
                        plan.getPlanId(),
                        item.getAssetCode().trim(),
                        item.getAssetType())
                .ifPresent(existing -> FinancialPlanError.FP_ASSET_DUPLICATED.throwsError(
                        "planId=" + plan.getPlanId()
                                + ", assetCode=" + item.getAssetCode()
                                + ", assetType=" + item.getAssetType()));

        FinancialPlanAsset entity = new FinancialPlanAsset();
        entity.setPlanId(plan.getPlanId());
        entity.setAssetType(item.getAssetType());
        entity.setAssetCode(item.getAssetCode().trim());
        entity.setAssetName(item.getAssetName().trim());
        entity.setStockSubType(resolveStockSubType(item, plan));
        entity.setPlanBuyPrice(item.getPlanBuyPrice());
        entity.setPlanSellPrice(item.getPlanSellPrice());
        entity.setPlanQuantity(item.getPlanQuantity());
        entity.setRealizedQuantity(BigDecimal.ZERO);
        entity.setCurrency(item.getCurrency().trim());
        entity.setSequenceNo(item.getSequenceNo());
        return entity;
    }

    /** 计算标的的股票子类型：优先取入参；缺省回退到计划级子类型。 */
    private StockSubType resolveStockSubType(SaveFinancialPlanAssetItem item, FinancialPlan plan) {
        if (item.getAssetType() != PlanType.STOCK) {
            return null;
        }
        return item.getStockSubType() != null ? item.getStockSubType() : plan.getStockSubType();
    }

    /** 拼接 (assetCode, assetType) 唯一键，用于请求体内部去重。 */
    private String buildUniqueKey(String assetCode, PlanType assetType) {
        return assetCode + "::" + assetType;
    }
}
