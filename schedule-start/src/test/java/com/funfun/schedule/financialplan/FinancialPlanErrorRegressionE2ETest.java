package com.funfun.schedule.financialplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.service.FinancialPlanStatsService;
import com.funfun.schedule.service.UserVipService;
import com.funfun.schedule.utils.LoginCheckUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4.3 异常与边界回归 E2E。
 *
 * <p>覆盖 design.md#3.1 中全部业务错误码及关键并发场景：
 * 计划、标的、批次三类乐观锁冲突。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class FinancialPlanErrorRegressionE2ETest {

    /**
     * 模拟登录校验，默认返回 group member 用户 1。
     */
    @MockBean
    private LoginCheckUtil loginCheckUtil;

    /**
     * 模拟 VIP 服务，避免外部依赖影响测试。
     */
    @MockBean
    private UserVipService userVipService;

    /**
     * 用于定向触发 FP_STAT_CALC_FAILED 异常路径。
     */
    @SpyBean
    private FinancialPlanStatsService financialPlanStatsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long GROUP_ID = 1L;
    private static final Long OWNER_USER_ID = 1L;

    /**
     * 初始化默认 mock 行为。
     */
    @BeforeEach
    void setUp() {
        when(loginCheckUtil.checkLoginAndGetUserId(any(HttpServletRequest.class))).thenReturn(1L);
        when(userVipService.getUserVip(any())).thenReturn(Optional.empty());
        reset(financialPlanStatsService);
    }

    /**
     * FP_PERMISSION_DENIED：非群组成员查询计划。
     */
    @Test
    @DisplayName("[ErrCode] FP_PERMISSION_DENIED")
    void error_permissionDenied_queryPlans() throws Exception {
        when(loginCheckUtil.checkLoginAndGetUserId(any(HttpServletRequest.class))).thenReturn(999L);

        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("pageNo", 1);
        body.put("pageSize", 10);

        mockMvc.perform(post("/api/financial-plans/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_PERMISSION_DENIED"));
    }

    /**
     * FP_QUERY_INVALID：分页参数超限。
     */
    @Test
    @DisplayName("[ErrCode] FP_QUERY_INVALID")
    void error_queryInvalid_pageSizeTooLarge() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("pageNo", 1);
        body.put("pageSize", 999);

        mockMvc.perform(post("/api/financial-plans/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_QUERY_INVALID"));
    }

    /**
     * FP_VALIDATION_FAILED：创建计划缺少必填字段。
     */
    @Test
    @DisplayName("[ErrCode] FP_VALIDATION_FAILED")
    void error_validationFailed_missingPlanName() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("ownerUserId", OWNER_USER_ID);
        body.put("planType", "SAVINGS");
        body.put("timeRangeType", "YEAR");
        body.put("fiscalYear", 2026);

        mockMvc.perform(post("/api/financial-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_VALIDATION_FAILED"));
    }

    /**
     * FP_WINDOW_INVALID：CUSTOM 模式下开始时间晚于结束时间。
     */
    @Test
    @DisplayName("[ErrCode] FP_WINDOW_INVALID")
    void error_windowInvalid_customDateRange() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("ownerUserId", OWNER_USER_ID);
        body.put("planName", "window-invalid");
        body.put("planType", "SAVINGS");
        body.put("timeRangeType", "CUSTOM");
        body.put("startDate", "2026-05-02");
        body.put("endDate", "2026-05-01");

        mockMvc.perform(post("/api/financial-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_WINDOW_INVALID"));
    }

    /**
     * FP_TYPE_UNSUPPORTED：股票计划缺少股票子类型。
     */
    @Test
    @DisplayName("[ErrCode] FP_TYPE_UNSUPPORTED")
    void error_typeUnsupported_stockWithoutSubType() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("ownerUserId", OWNER_USER_ID);
        body.put("planName", "type-invalid");
        body.put("planType", "STOCK");
        body.put("timeRangeType", "YEAR");
        body.put("fiscalYear", 2026);

        mockMvc.perform(post("/api/financial-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_TYPE_UNSUPPORTED"));
    }

    /**
     * FP_PLAN_NOT_FOUND：查询不存在计划。
     */
    @Test
    @DisplayName("[ErrCode] FP_PLAN_NOT_FOUND")
    void error_planNotFound_getDetail() throws Exception {
        mockMvc.perform(get("/api/financial-plans/{planId}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_PLAN_NOT_FOUND"));
    }

    /**
     * FP_VERSION_CONFLICT（计划并发）：用过期版本更新计划。
     */
    @Test
    @DisplayName("[ErrCode][Concurrency] FP_VERSION_CONFLICT on Plan")
    void error_versionConflict_planUpdate() throws Exception {
        Long planId = createPlan("version-plan", "SAVINGS", null, "YEAR", 2026);
        int version = getPlanVersion(planId);

        Map<String, Object> firstUpdate = new HashMap<>();
        firstUpdate.put("planName", "first-update");
        firstUpdate.put("timeRangeType", "YEAR");
        firstUpdate.put("fiscalYear", 2026);
        firstUpdate.put("version", version);

        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Map<String, Object> staleUpdate = new HashMap<>();
        staleUpdate.put("planName", "stale-update");
        staleUpdate.put("timeRangeType", "YEAR");
        staleUpdate.put("fiscalYear", 2026);
        staleUpdate.put("version", version);

        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_VERSION_CONFLICT"));
    }

    /**
     * FP_ASSET_INVALID：标的参数非法。
     */
    @Test
    @DisplayName("[ErrCode] FP_ASSET_INVALID")
    void error_assetInvalid_negativeQuantity() throws Exception {
        Long planId = createPlan("asset-invalid-plan", "STOCK", "EQUITY", "YEAR", 2026);

        Map<String, Object> item = new HashMap<>();
        item.put("assetType", "STOCK");
        item.put("assetCode", "AAPL");
        item.put("assetName", "Apple");
        item.put("stockSubType", "EQUITY");
        item.put("planBuyPrice", "100");
        item.put("planSellPrice", "120");
        item.put("planQuantity", "-1");
        item.put("currency", "USD");
        item.put("sequenceNo", 1);

        Map<String, Object> req = new HashMap<>();
        req.put("items", List.of(item));

        mockMvc.perform(post("/api/financial-plans/{planId}/assets/save", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_ASSET_INVALID"));
    }

    /**
     * FP_ASSET_DUPLICATED：同一请求重复标的。
     */
    @Test
    @DisplayName("[ErrCode] FP_ASSET_DUPLICATED")
    void error_assetDuplicated_sameRequest() throws Exception {
        Long planId = createPlan("asset-dup-plan", "SAVINGS", null, "YEAR", 2026);

        Map<String, Object> item1 = buildAssetItem("SAVINGS", "BANK-001", "Bank", "1", "1.1", "100", 1);
        Map<String, Object> item2 = buildAssetItem("SAVINGS", "BANK-001", "Bank", "1", "1.1", "100", 2);

        Map<String, Object> req = new HashMap<>();
        req.put("items", List.of(item1, item2));

        mockMvc.perform(post("/api/financial-plans/{planId}/assets/save", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_ASSET_DUPLICATED"));
    }

    /**
     * FP_ASSET_NOT_FOUND：更新不存在标的。
     */
    @Test
    @DisplayName("[ErrCode] FP_ASSET_NOT_FOUND")
    void error_assetNotFound_updateParams() throws Exception {
        Long planId = createPlan("asset-not-found", "SAVINGS", null, "YEAR", 2026);

        Map<String, Object> body = new HashMap<>();
        body.put("planBuyPrice", "1.0");
        body.put("planSellPrice", "1.2");
        body.put("planQuantity", "50");
        body.put("version", 0);

        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_ASSET_NOT_FOUND"));
    }

    /**
     * FP_ASSET_QTY_LT_REALIZED：计划数量小于已兑现数量。
     */
    @Test
    @DisplayName("[ErrCode] FP_ASSET_QTY_LT_REALIZED")
    void error_assetQtyLtRealized() throws Exception {
        Long planId = createPlan("qty-lt-realized", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "TSLA", "Tesla", "200", "280", "100");
        Long batchId = createBatch(planId, assetId, "60", "batch-1");

        performBuy(planId, batchId, 0, "210.0", "60", "5.0");
        performSell(planId, batchId, 1, "260.0", "60", "6.0");

        int assetVersion = getAssetVersion(planId, assetId);
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("planBuyPrice", "200.0");
        updateReq.put("planSellPrice", "280.0");
        updateReq.put("planQuantity", "50");
        updateReq.put("version", assetVersion);

        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_ASSET_QTY_LT_REALIZED"));
    }

    /**
     * FP_REALIZATION_QTY_EXCEEDED：批次数量超额。
     */
    @Test
    @DisplayName("[ErrCode] FP_REALIZATION_QTY_EXCEEDED")
    void error_realizationQtyExceeded() throws Exception {
        Long planId = createPlan("realization-over", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "MSFT", "Microsoft", "300", "380", "100");

        Map<String, Object> batchReq = new HashMap<>();
        batchReq.put("assetId", assetId);
        batchReq.put("batchName", "over-batch");
        batchReq.put("quantity", "101");

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_REALIZATION_QTY_EXCEEDED"));
    }

    /**
     * FP_BATCH_NOT_FOUND：登记买入时批次不存在。
     */
    @Test
    @DisplayName("[ErrCode] FP_BATCH_NOT_FOUND")
    void error_batchNotFound_recordBuy() throws Exception {
        Long planId = createPlan("batch-not-found", "STOCK", "EQUITY", "YEAR", 2026);

        Map<String, Object> body = new HashMap<>();
        body.put("tradeDate", "2026-02-15");
        body.put("actualBuyPrice", "10.0");
        body.put("quantity", "1");
        body.put("fee", "0");
        body.put("version", 0);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/buy", planId, 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_BATCH_NOT_FOUND"));
    }

    /**
     * FP_STAGE_CONFLICT：批次已完成后再次卖出。
     */
    @Test
    @DisplayName("[ErrCode] FP_STAGE_CONFLICT")
    void error_stageConflict_sellAfterCompleted() throws Exception {
        Long planId = createPlan("stage-conflict", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "AMZN", "Amazon", "100", "150", "20");
        Long batchId = createBatch(planId, assetId, "20", "batch-stage");

        performBuy(planId, batchId, 0, "105.0", "20", "1.0");
        performSell(planId, batchId, 1, "140.0", "20", "1.0");

        Map<String, Object> sellReq = new HashMap<>();
        sellReq.put("tradeDate", "2026-03-20");
        sellReq.put("actualSellPrice", "141.0");
        sellReq.put("quantity", "1");
        sellReq.put("fee", "0");
        sellReq.put("version", 2);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/sell", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_STAGE_CONFLICT"));
    }

    /**
     * FP_SELL_BEFORE_BUY：先卖后买。
     */
    @Test
    @DisplayName("[ErrCode] FP_SELL_BEFORE_BUY")
    void error_sellBeforeBuy() throws Exception {
        Long planId = createPlan("sell-before-buy", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "BABA", "Alibaba", "80", "100", "30");
        Long batchId = createBatch(planId, assetId, "30", "batch-sell-first");

        Map<String, Object> sellReq = new HashMap<>();
        sellReq.put("tradeDate", "2026-03-01");
        sellReq.put("actualSellPrice", "90.0");
        sellReq.put("quantity", "30");
        sellReq.put("fee", "1.0");
        sellReq.put("version", 0);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/sell", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_SELL_BEFORE_BUY"));
    }

    /**
     * FP_PLAN_ALREADY_ARCHIVED：归档后再次写操作。
     */
    @Test
    @DisplayName("[ErrCode] FP_PLAN_ALREADY_ARCHIVED")
    void error_planAlreadyArchived_updateAfterArchive() throws Exception {
        Long planId = createPlan("archive-protect", "SAVINGS", null, "YEAR", 2026);
        int version = getPlanVersion(planId);

        Map<String, Object> archiveReq = new HashMap<>();
        archiveReq.put("version", version);
        mockMvc.perform(delete("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("planName", "should-fail");
        updateReq.put("timeRangeType", "YEAR");
        updateReq.put("fiscalYear", 2026);
        updateReq.put("version", version + 1);

        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_PLAN_ALREADY_ARCHIVED"));
    }

    /**
     * FP_VERSION_CONFLICT（标的并发）：过期版本更新标的。
     */
    @Test
    @DisplayName("[ErrCode][Concurrency] FP_VERSION_CONFLICT on Asset")
    void error_versionConflict_assetUpdate() throws Exception {
        Long planId = createPlan("asset-version", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "NFLX", "Netflix", "300", "360", "40");
        int version = getAssetVersion(planId, assetId);

        Map<String, Object> firstReq = new HashMap<>();
        firstReq.put("planBuyPrice", "305.0");
        firstReq.put("planSellPrice", "365.0");
        firstReq.put("planQuantity", "40");
        firstReq.put("version", version);

        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Map<String, Object> staleReq = new HashMap<>();
        staleReq.put("planBuyPrice", "310.0");
        staleReq.put("planSellPrice", "370.0");
        staleReq.put("planQuantity", "40");
        staleReq.put("version", version);

        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_VERSION_CONFLICT"));
    }

    /**
     * FP_VERSION_CONFLICT（批次并发）：过期版本登记批次买入。
     */
    @Test
    @DisplayName("[ErrCode][Concurrency] FP_VERSION_CONFLICT on Batch")
    void error_versionConflict_batchUpdate() throws Exception {
        Long planId = createPlan("batch-version", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "ORCL", "Oracle", "100", "130", "20");
        Long batchId = createBatch(planId, assetId, "20", "batch-version-1");

        Map<String, Object> firstBuy = new HashMap<>();
        firstBuy.put("tradeDate", "2026-02-10");
        firstBuy.put("actualBuyPrice", "101.0");
        firstBuy.put("quantity", "20");
        firstBuy.put("fee", "1.0");
        firstBuy.put("version", 0);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/buy", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBuy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Map<String, Object> staleBuy = new HashMap<>();
        staleBuy.put("tradeDate", "2026-02-11");
        staleBuy.put("actualBuyPrice", "102.0");
        staleBuy.put("quantity", "1");
        staleBuy.put("fee", "0");
        staleBuy.put("version", 0);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/buy", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleBuy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_VERSION_CONFLICT"));
    }

    /**
     * FP_STAT_CALC_FAILED：统计服务异常路径回归。
     */
    @Test
    @DisplayName("[ErrCode] FP_STAT_CALC_FAILED")
    void error_statCalcFailed_dashboard() throws Exception {
        Long planId = createPlan("stat-fail-plan", "SAVINGS", null, "YEAR", 2026);
        doThrow(new MyException("FP_STAT_CALC_FAILED", "mock stat calc failed"))
                .when(financialPlanStatsService)
                .calcProgressSnapshot(planId);

        mockMvc.perform(get("/api/financial-plans/{planId}/dashboard", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_STAT_CALC_FAILED"));

        reset(financialPlanStatsService);
    }

    /**
     * 创建理财计划并返回 planId。
     */
    private Long createPlan(String planName, String planType, String stockSubType,
                            String timeRangeType, int fiscalYear) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("ownerUserId", OWNER_USER_ID);
        body.put("planName", planName);
        body.put("planType", planType);
        if (stockSubType != null) {
            body.put("stockSubType", stockSubType);
        }
        body.put("timeRangeType", timeRangeType);
        body.put("fiscalYear", fiscalYear);

        String resp = mockMvc.perform(post("/api/financial-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        return asLong(data.get("planId"));
    }

    /**
     * 保存单个标的并返回 assetId。
     */
    private Long saveFirstAsset(Long planId, String assetCode, String assetName,
                                String buyPrice, String sellPrice, String qty) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildAssetItem("STOCK", assetCode, assetName, buyPrice, sellPrice, qty, 1));
        Map<String, Object> req = new HashMap<>();
        req.put("items", items);

        String resp = mockMvc.perform(post("/api/financial-plans/{planId}/assets/save", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        List<?> savedItems = (List<?>) data.get("items");
        Map<?, ?> firstItem = (Map<?, ?>) savedItems.get(0);
        return asLong(firstItem.get("assetId"));
    }

    /**
     * 构建标的请求对象。
     */
    private Map<String, Object> buildAssetItem(String assetType,
                                               String assetCode,
                                               String assetName,
                                               String buyPrice,
                                               String sellPrice,
                                               String qty,
                                               int sequenceNo) {
        Map<String, Object> item = new HashMap<>();
        item.put("assetType", assetType);
        item.put("assetCode", assetCode);
        item.put("assetName", assetName);
        if ("STOCK".equals(assetType)) {
            item.put("stockSubType", "EQUITY");
        }
        item.put("planBuyPrice", buyPrice);
        item.put("planSellPrice", sellPrice);
        item.put("planQuantity", qty);
        item.put("currency", "USD");
        item.put("sequenceNo", sequenceNo);
        return item;
    }

    /**
     * 创建兑现批次并返回 batchId。
     */
    private Long createBatch(Long planId, Long assetId, String qty, String batchName) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("assetId", assetId);
        body.put("batchName", batchName);
        body.put("quantity", qty);

        String resp = mockMvc.perform(post("/api/financial-plans/{planId}/realizations", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        return asLong(data.get("batchId"));
    }

    /**
     * 登记买入。
     */
    private void performBuy(Long planId, Long batchId, int version,
                            String price, String qty, String fee) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tradeDate", "2026-02-15");
        body.put("actualBuyPrice", price);
        body.put("quantity", qty);
        body.put("fee", fee);
        body.put("version", version);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/buy", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    /**
     * 登记卖出。
     */
    private void performSell(Long planId, Long batchId, int version,
                             String price, String qty, String fee) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tradeDate", "2026-03-15");
        body.put("actualSellPrice", price);
        body.put("quantity", qty);
        body.put("fee", fee);
        body.put("version", version);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/sell", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    /**
     * 从详情中读取计划版本。
     */
    private int getPlanVersion(Long planId) throws Exception {
        String resp = mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        Map<?, ?> plan = (Map<?, ?>) data.get("plan");
        return asInt(plan.get("version"));
    }

    /**
     * 从详情中读取标的版本。
     */
    private int getAssetVersion(Long planId, Long assetId) throws Exception {
        String resp = mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        List<?> assets = (List<?>) data.get("assets");
        for (Object asset : assets) {
            Map<?, ?> current = (Map<?, ?>) asset;
            if (asLong(current.get("assetId")).equals(assetId)) {
                return asInt(current.get("version"));
            }
        }
        throw new IllegalStateException("Asset not found for version lookup: assetId=" + assetId);
    }

    /**
     * 转 Long。
     */
    private Long asLong(Object value) {
        if (value == null) {
            throw new IllegalStateException("Expected non-null numeric value");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 转 int。
     */
    private int asInt(Object value) {
        if (value == null) {
            throw new IllegalStateException("Expected non-null numeric value");
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
