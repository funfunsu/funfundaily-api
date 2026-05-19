package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.SaveFinancialPlanAssetItem;
import com.funfun.schedule.dto.UpdateFinancialPlanAssetParamsCommand;
import com.funfun.schedule.entity.FinancialPlan;
import com.funfun.schedule.entity.FinancialPlanAsset;
import com.funfun.schedule.enums.AssetMarket;
import com.funfun.schedule.enums.PlanStatus;
import com.funfun.schedule.exception.FinancialPlanError;
import com.funfun.schedule.repository.FinancialPlanAssetRepository;
import com.funfun.schedule.repository.FinancialPlanRepository;
import com.funfun.schedule.service.PlanAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 计划标的领域服务实现。
 *
 * <p>新模型下，标的仅承载「股票名 + 市场 + 目标利润」三件套。
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

        // 请求体内自校验：同一请求中 (stockName, market) 不可重复。
        Set<String> uniqueKeys = new HashSet<>();
        for (SaveFinancialPlanAssetItem item : items) {
            validateAssetItem(item);
            String key = buildUniqueKey(item.getStockName(), item.getMarket());
            if (!uniqueKeys.add(key)) {
                FinancialPlanError.FP_ASSET_DUPLICATED.throwsError(key);
            }
        }

        for (SaveFinancialPlanAssetItem item : items) {
            FinancialPlanAsset entity = upsertAsset(plan, item);
            try {
                financialPlanAssetRepository.save(entity);
            } catch (OptimisticLockingFailureException ex) {
                FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                        "assetId=" + entity.getAssetId());
            }
        }

        return financialPlanAssetRepository
                .findByPlanIdAndDeletedFalseOrderBySequenceNoAsc(planId);
    }

    /** 调整单个标的的目标利润等字段。 */
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

        if (command.getStockName() != null && !command.getStockName().trim().isEmpty()) {
            asset.setStockName(command.getStockName().trim());
        }
        if (command.getMarket() != null) {
            asset.setMarket(command.getMarket());
        }
        if (command.getTargetProfit() != null) {
            asset.setTargetProfit(command.getTargetProfit());
        }

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

    // ===================== 内部工具 =====================

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

    private void ensureAssetBelongsToPlan(FinancialPlanAsset asset, Long planId) {
        if (!Objects.equals(asset.getPlanId(), planId)) {
            FinancialPlanError.FP_ASSET_NOT_FOUND.throwsError(
                    "assetId=" + asset.getAssetId() + ", planId=" + planId);
        }
    }

    private void ensureVersionMatches(FinancialPlanAsset asset, Integer expectedVersion) {
        if (expectedVersion == null) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError("missing version");
        }
        if (!Objects.equals(asset.getVersion(), expectedVersion)) {
            FinancialPlanError.FP_VERSION_CONFLICT.throwsError(
                    "expected=" + expectedVersion + ", actual=" + asset.getVersion());
        }
    }

    /** 标的入参校验：股票名 / 市场 / 目标盈利（已计划/已实现盈利由批次推导）。 */
    private void validateAssetItem(SaveFinancialPlanAssetItem item) {
        if (item == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("item is null");
        }
        if (item.getStockName() == null || item.getStockName().trim().isEmpty()) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("stockName is required");
        }
        if (item.getMarket() == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("market is required");
        }
        if (item.getTargetProfit() == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("targetProfit is required");
        }
        if (item.getSequenceNo() == null) {
            FinancialPlanError.FP_ASSET_INVALID.throwsError("sequenceNo is required");
        }
    }

    /** 新增或更新单个标的。 */
    private FinancialPlanAsset upsertAsset(FinancialPlan plan, SaveFinancialPlanAssetItem item) {
        if (item.getAssetId() != null) {
            FinancialPlanAsset existing = loadAssetForWrite(item.getAssetId());
            ensureAssetBelongsToPlan(existing, plan.getPlanId());

            existing.setStockName(item.getStockName().trim());
            existing.setMarket(item.getMarket());
            existing.setTargetProfit(item.getTargetProfit());
            existing.setSequenceNo(item.getSequenceNo());
            return existing;
        }

        // 新增前去重：同计划下同 (stockName, market) 不可重复。
        financialPlanAssetRepository
                .findByPlanIdAndStockNameAndMarketAndDeletedFalse(
                        plan.getPlanId(), item.getStockName().trim(), item.getMarket())
                .ifPresent(dup -> FinancialPlanError.FP_ASSET_DUPLICATED.throwsError(
                        "stockName=" + item.getStockName() + ", market=" + item.getMarket()));

        FinancialPlanAsset entity = new FinancialPlanAsset();
        entity.setPlanId(plan.getPlanId());
        entity.setStockName(item.getStockName().trim());
        entity.setMarket(item.getMarket());
        entity.setTargetProfit(item.getTargetProfit());
        entity.setSequenceNo(item.getSequenceNo());
        return entity;
    }

    /** 拼接同请求体内 (stockName, market) 唯一键，用于自校验。 */
    private String buildUniqueKey(String stockName, AssetMarket market) {
        return (stockName == null ? "" : stockName.trim())
                + "|" + (market == null ? "" : market.name());
    }

    /** 新增的批次写入时间戳辅助；与公共错误处理保持一致，不重复定义。 */
    @SuppressWarnings("unused")
    private static <T> ArrayList<T> emptyList() {
        return new ArrayList<>();
    }
}
