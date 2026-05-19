package com.funfun.schedule.financialplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.service.UserVipService;
import com.funfun.schedule.utils.LoginCheckUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class FinancialPlanMainFlowE2ETest {

    @MockBean
    private LoginCheckUtil loginCheckUtil;

    @MockBean
    private UserVipService userVipService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long GROUP_ID = 1L;
    private static final Long OWNER_USER_ID = 1L;

    @BeforeEach
    /**
     * Mock login/vip dependencies so E2E flows can run in isolation.
     */
    void setUp() {
        when(loginCheckUtil.checkLoginAndGetUserId(any(HttpServletRequest.class))).thenReturn(1L);
        when(userVipService.getUserVip(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("[Req-1][Req-2] E2E 主链路-1: 创建计划并添加多个标的")
    /**
     * Main flow 1: create a plan and save multiple assets.
     */
    void e2e_case1_createPlanAndAddMultipleAssets() throws Exception {
        Long planId = createPlan("E2E-主链路1-计划", "STOCK", "EQUITY", "YEAR", 2026);

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildAssetItem("AAPL", "Apple", "150", "190", "80", 1));
        items.add(buildAssetItem("MSFT", "Microsoft", "280", "340", "120", 2));

        Map<String, Object> req = new HashMap<>();
        req.put("items", items);

        String saveResp = mockMvc.perform(post("/api/financial-plans/{planId}/assets/save", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(saveResp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        List<?> savedItems = (List<?>) data.get("items");
        assertEquals(2, savedItems.size());
    }

    @Test
    @DisplayName("[Req-2][Req-3][Req-4] E2E 主链路-2: 多批次兑现并分步登记买入卖出")
    /**
     * Main flow 2: create multi-batches and complete buy/sell in steps.
     */
    void e2e_case2_multiBatchBuySell() throws Exception {
        Long planId = createPlan("E2E-主链路2-计划", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "NVDA", "NVIDIA", "500", "680", "300");

        Long batch1 = createBatch(planId, assetId, "100", "第一批");
        Long batch2 = createBatch(planId, assetId, "120", "第二批");

        performBuy(planId, batch1, 0, "510.0", "100", "15.0");
        performBuy(planId, batch2, 0, "520.0", "120", "20.0");

        performSell(planId, batch1, 1, "640.0", "100", "18.0");
        performSell(planId, batch2, 1, "660.0", "120", "22.0");

        String detailResp = mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailResult = objectMapper.readValue(detailResp, Map.class);
        Map<?, ?> detailData = (Map<?, ?>) detailResult.get("data");
        List<?> batches = (List<?>) detailData.get("realizationBatches");
        assertTrue(batches.size() >= 2);
    }

    @Test
    @DisplayName("[Req-2][Req-4] E2E 主链路-3: 调整计划价格和数量后刷新统计")
    /**
     * Main flow 3: update asset params and verify dashboard statistics.
     */
    void e2e_case3_adjustAssetAndRefreshStats() throws Exception {
        Long planId = createPlan("E2E-主链路3-计划", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "TSLA", "Tesla", "210", "290", "100");

        int assetVersion = getAssetVersion(planId, assetId);
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("planBuyPrice", "220.0");
        updateBody.put("planSellPrice", "310.0");
        updateBody.put("planQuantity", "120");
        updateBody.put("version", assetVersion);

        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        String dashboardResp = mockMvc.perform(get("/api/financial-plans/{planId}/dashboard", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> dashboardResult = objectMapper.readValue(dashboardResp, Map.class);
        Map<?, ?> dashboardData = (Map<?, ?>) dashboardResult.get("data");
        Map<?, ?> planSummary = (Map<?, ?>) dashboardData.get("planSummary");
        assertNotNull(planSummary);
        BigDecimal targetProfit = new BigDecimal(String.valueOf(planSummary.get("targetProfit")));
        assertTrue(targetProfit.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("[Req-1][Req-5] E2E 主链路-4: 按状态与时间窗口筛选并查看历史复盘")
    /**
     * Main flow 4: filter archived plans and ensure archived plans are read-only.
     */
    void e2e_case4_filterByStatusAndReviewArchivedPlan() throws Exception {
        Long planId = createPlan("E2E-主链路4-计划", "SAVINGS", null, "YEAR", 2026);
        int planVersion = getPlanVersion(planId);

        Map<String, Object> archiveReq = new HashMap<>();
        archiveReq.put("version", planVersion);
        mockMvc.perform(delete("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Map<String, Object> queryReq = new HashMap<>();
        queryReq.put("groupId", GROUP_ID);
        queryReq.put("executionStatus", "ARCHIVED");
        queryReq.put("timeRangeType", "YEAR");
        queryReq.put("startDate", "2026-01-01");
        queryReq.put("endDate", "2026-12-31");
        queryReq.put("pageNo", 1);
        queryReq.put("pageSize", 20);

        mockMvc.perform(post("/api/financial-plans/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.total").exists());

        mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        int archivedVersion = getPlanVersion(planId);
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("planName", "归档后编辑应失败");
        updateReq.put("timeRangeType", "YEAR");
        updateReq.put("fiscalYear", 2026);
        updateReq.put("version", archivedVersion);

        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_PLAN_ALREADY_ARCHIVED"));
    }

    /**
     * Create a plan through API and return its planId.
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
     * Save one asset and return the first assetId.
     */
    private Long saveFirstAsset(Long planId, String assetCode, String assetName,
                                String buyPrice, String sellPrice, String qty) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildAssetItem(assetCode, assetName, buyPrice, sellPrice, qty, 1));
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
     * Build one stock asset payload item.
     */
    private Map<String, Object> buildAssetItem(String assetCode, String assetName,
                                               String buyPrice, String sellPrice,
                                               String qty, int sequenceNo) {
        Map<String, Object> item = new HashMap<>();
        item.put("assetType", "STOCK");
        item.put("assetCode", assetCode);
        item.put("assetName", assetName);
        item.put("stockSubType", "EQUITY");
        item.put("planBuyPrice", buyPrice);
        item.put("planSellPrice", sellPrice);
        item.put("planQuantity", qty);
        item.put("currency", "USD");
        item.put("sequenceNo", sequenceNo);
        return item;
    }

    /**
     * Create one realization batch and return batchId.
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
     * Record one buy operation for a batch.
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
     * Record one sell operation for a batch and assert completed status.
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
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.stageStatus").value("COMPLETED"));
    }

    /**
     * Read asset version from plan detail response.
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
     * Read current plan version from plan detail response.
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
     * Convert value into Long.
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
     * Convert value into int.
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
