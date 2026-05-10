package com.funfun.schedule.financialplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.service.UserVipService;
import com.funfun.schedule.utils.LoginCheckUtil;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 理财计划 API 集成测试（Task 2.7）。
 *
 * <p>覆盖范围：
 * <ul>
 *   <li>API-1  POST /api/financial-plans/query  — 分页查询</li>
 *   <li>API-2  POST /api/financial-plans         — 创建计划</li>
 *   <li>API-3  PUT  /api/financial-plans/{id}    — 更新计划</li>
 *   <li>API-4  POST /api/financial-plans/{id}/assets/save — 批量保存标的</li>
 *   <li>API-5  PUT  /api/financial-plans/{id}/assets/{aid} — 更新标的参数</li>
 *   <li>API-6  POST /api/financial-plans/{id}/realizations          — 创建批次</li>
 *   <li>API-7  POST /api/financial-plans/{id}/realizations/{bid}/buy  — 登记买入</li>
 *   <li>API-8  POST /api/financial-plans/{id}/realizations/{bid}/sell — 登记卖出</li>
 *   <li>API-9  GET  /api/financial-plans/{id}             — 计划详情</li>
 *   <li>API-10 GET  /api/financial-plans/{id}/dashboard   — 统计 Dashboard</li>
 *   <li>主流程端到端链条测试</li>
 *   <li>错误路径：归档后编辑、先卖后买、数量超标、版本冲突、计划不存在</li>
 * </ul>
 *
 * <p>技术说明：
 * <ul>
 *   <li>使用 H2 内存库（dev profile），Hibernate create-drop 建表，data.sql 预置用户/群组/成员种子数据。</li>
 *   <li>{@code @MockBean LoginCheckUtil}：绕过 JWT 校验，固定返回 userId=1。</li>
 *   <li>{@code @MockBean UserVipService}：避免 VIP 查询干扰。</li>
 *   <li>使用 {@code @Transactional} 回滚隔离每个用例，避免软删除清理差异。</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class FinancialPlanApiIntegrationTest {

    // =================== 依赖注入 ===================

    /** 模拟 JWT 校验，固定返回 userId=1（与 data.sql group_member 种子行对齐）。 */
    @MockBean
    private LoginCheckUtil loginCheckUtil;

    /** 模拟 VIP 查询，避免外部依赖影响测试结果。 */
    @MockBean
    private UserVipService userVipService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 测试群组 ID，与 data.sql 种子数据中 group_member.group_id 对齐。 */
    private static final Long GROUP_ID = 1L;

    /** 测试负责人用户 ID，与 data.sql 种子数据中 group_member.user_id 对齐。 */
    private static final Long OWNER_USER_ID = 1L;

    // =================== 测试初始化 ===================

    @BeforeEach
    void setUp() {
        // 模拟 AuthInterceptor：JWT 校验通过，返回 userId=1
        when(loginCheckUtil.checkLoginAndGetUserId(any(HttpServletRequest.class))).thenReturn(1L);
        // 模拟 VIP 查询，避免 VIP 逻辑干扰
        when(userVipService.getUserVip(any())).thenReturn(Optional.empty());
    }

    // =================== API-1：分页查询 ===================

    /**
     * API-1 正常场景：创建 2 个计划后分页查询，返回 total=2。
     */
    @Test
    void api1_queryPlans_returnsPagedList() throws Exception {
        createPlan("查询计划A", "SAVINGS", null, "YEAR", 2026);
        createPlan("查询计划B", "SAVINGS", null, "YEAR", 2026);

        Map<String, Object> query = new HashMap<>();
        query.put("groupId", GROUP_ID);
        query.put("pageNo", 1);
        query.put("pageSize", 10);

        mockMvc.perform(post("/api/financial-plans/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    // =================== API-2：创建计划 ===================

    /**
     * API-2 正常场景：创建股票类型计划，返回 planId 且初始 version=0。
     */
    @Test
    void api2_createPlan_returnsNewPlanId() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("ownerUserId", OWNER_USER_ID);
        body.put("planName", "2026年度股票计划");
        body.put("planType", "STOCK");
        body.put("stockSubType", "EQUITY");
        body.put("timeRangeType", "YEAR");
        body.put("fiscalYear", 2026);
        body.put("remark", "集成测试备注");

        mockMvc.perform(post("/api/financial-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.planId").isString())
                .andExpect(jsonPath("$.data.version").value(0));
    }

    // =================== API-3：更新计划 ===================

    /**
     * API-3 正常场景：更新计划名称，version 递增。
     */
    @Test
    void api3_updatePlan_success() throws Exception {
        Long planId = createPlan("原计划名", "SAVINGS", null, "YEAR", 2026);
        int version = getPlanVersion(planId); // = 0

        Map<String, Object> body = new HashMap<>();
        body.put("planName", "更新后计划名");
        body.put("timeRangeType", "YEAR");
        body.put("fiscalYear", 2026);
        body.put("version", version);

        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.planId").value(planId));
    }

    // =================== API-4：批量保存标的 ===================

    /**
     * API-4 正常场景：保存单个标的，返回含 assetId 的标的列表。
     */
    @Test
    void api4_saveAssets_returnsAssetList() throws Exception {
        Long planId = createPlan("标的测试计划", "STOCK", "EQUITY", "YEAR", 2026);
        List<Map<String, Object>> items = buildAssetItems("BABA", "阿里巴巴", "100", "130", "500");

        Map<String, Object> req = new HashMap<>();
        req.put("items", items);

        mockMvc.perform(post("/api/financial-plans/{planId}/assets/save", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.items[0].assetId").isString())
                .andExpect(jsonPath("$.data.items[0].assetCode").value("BABA"));
    }

    // =================== API-5：更新标的参数 ===================

    /**
     * API-5 正常场景：调整买卖价格，返回 targetProfit。
     */
    @Test
    void api5_updateAssetParams_returnsTargetProfit() throws Exception {
        Long planId = createPlan("标的更新计划", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "AAPL", "苹果", "150", "200", "100");
        int assetVersion = getAssetVersion(planId, assetId);

        Map<String, Object> body = new HashMap<>();
        body.put("planBuyPrice", "155.0");
        body.put("planSellPrice", "210.0");
        body.put("planQuantity", "100");
        body.put("version", assetVersion);

        // targetProfit = (210 - 155) * 100 = 5500
        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.assetId").value(assetId))
                .andExpect(jsonPath("$.data.targetProfit").isNumber());
    }

    // =================== API-6：创建兑现批次 ===================

    /**
     * API-6 正常场景：创建批次，初始阶段状态为 PENDING_BUY。
     */
    @Test
    void api6_createBatch_pendingBuyStatus() throws Exception {
        Long planId = createPlan("批次测试计划", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "MSFT", "微软", "300", "400", "200");

        Map<String, Object> body = new HashMap<>();
        body.put("assetId", assetId);
        body.put("batchName", "第一批");
        body.put("quantity", "100");

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.batchId").isString())
                .andExpect(jsonPath("$.data.stageStatus").value("PENDING_BUY"));
    }

    // =================== API-7：登记买入 ===================

    /**
     * API-7 正常场景：登记买入，actualBuyAmount 不为空。
     */
    @Test
    void api7_recordBuy_statusUpdated() throws Exception {
        Long planId = createPlan("买入测试计划", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "GOOG", "谷歌", "140", "180", "100");
        Long batchId = createBatch(planId, assetId, "100");

        Map<String, Object> body = new HashMap<>();
        body.put("tradeDate", "2026-02-15");
        body.put("actualBuyPrice", "145.0");
        body.put("quantity", "100");
        body.put("fee", "50.0");
        body.put("version", 0); // 批次初始 version=0

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/buy", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.batchId").value(batchId))
                .andExpect(jsonPath("$.data.actualBuyAmount").isNumber());
    }

    // =================== API-8：登记卖出 ===================

    /**
     * API-8 正常场景：买入后登记卖出，批次状态变为 COMPLETED，actualProfit 有值。
     */
    @Test
    void api8_recordSell_batchCompleted() throws Exception {
        Long planId = createPlan("卖出测试计划", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "TSLA", "特斯拉", "200", "280", "50");
        Long batchId = createBatch(planId, assetId, "50");

        // 先买入（version=0）
        performBuy(planId, batchId, 0, "210.0", "50", "30.0");

        // 再卖出（buy 完成后 version=1）
        Map<String, Object> sellBody = new HashMap<>();
        sellBody.put("tradeDate", "2026-03-20");
        sellBody.put("actualSellPrice", "275.0");
        sellBody.put("quantity", "50");
        sellBody.put("fee", "35.0");
        sellBody.put("version", 1);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/sell", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.stageStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.actualProfit").isNumber());
    }

    // =================== API-9：计划详情 ===================

    /**
     * API-9 正常场景：查询计划详情，返回 plan / assets / realizationBatches 三段数据。
     */
    @Test
    void api9_getPlanDetail_success() throws Exception {
        Long planId = createPlan("详情测试计划", "SAVINGS", null, "YEAR", 2026);

        mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.plan.planId").value(planId))
                .andExpect(jsonPath("$.data.assets").isArray())
                .andExpect(jsonPath("$.data.realizationBatches").isArray());
    }

    // =================== API-10：Dashboard ===================

    /**
     * API-10 正常场景：查询 Dashboard 统计数据，响应码为 0。
     */
    @Test
    void api10_getDashboard_success() throws Exception {
        Long planId = createPlan("Dashboard测试计划", "SAVINGS", null, "YEAR", 2026);

        mockMvc.perform(get("/api/financial-plans/{planId}/dashboard", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").exists());
    }

    // =================== 主流程端到端链条 ===================

    /**
     * 主流程集成测试：
     * 创建计划(API-2) → 保存标的(API-4) → 更新标的参数(API-5) →
     * 创建批次(API-6) → 登记买入(API-7) → 登记卖出(API-8) →
     * 查询详情(API-9) → Dashboard(API-10)。
     *
     * <p>验证数据在各层之间的正确传递与持久化；最终批次状态应为 COMPLETED。
     */
    @Test
    void mainFlow_createThroughSellToDashboard() throws Exception {
        // Step 1: 创建计划（API-2）
        Long planId = createPlan("主流程集成测试计划", "STOCK", "EQUITY", "YEAR", 2026);

        // Step 2: 保存标的（API-4）
        Long assetId = saveFirstAsset(planId, "NVDA", "英伟达", "500", "700", "100");

        // Step 3: 更新标的参数（API-5）
        int assetVersion = getAssetVersion(planId, assetId);
        Map<String, Object> updateAssetBody = new HashMap<>();
        updateAssetBody.put("planBuyPrice", "510.0");
        updateAssetBody.put("planSellPrice", "720.0");
        updateAssetBody.put("planQuantity", "100");
        updateAssetBody.put("version", assetVersion);
        mockMvc.perform(put("/api/financial-plans/{planId}/assets/{assetId}", planId, assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAssetBody)))
                .andExpect(jsonPath("$.code").value("0"));

        // Step 4: 创建兑现批次（API-6）
        Long batchId = createBatch(planId, assetId, "100");

        // Step 5: 登记买入（API-7），批次初始 version=0
        performBuy(planId, batchId, 0, "515.0", "100", "100.0");

        // Step 6: 登记卖出（API-8），买入后 version=1
        Map<String, Object> sellBody = new HashMap<>();
        sellBody.put("tradeDate", "2026-06-01");
        sellBody.put("actualSellPrice", "715.0");
        sellBody.put("quantity", "100");
        sellBody.put("fee", "120.0");
        sellBody.put("version", 1);
        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/sell", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellBody)))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.stageStatus").value("COMPLETED"));

        // Step 7: 查询详情（API-9），验证批次状态持久化
        mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.plan.planId").value(planId))
                .andExpect(jsonPath("$.data.realizationBatches[0].stageStatus").value("COMPLETED"));

        // Step 8: Dashboard（API-10），验证统计端点正常响应
        mockMvc.perform(get("/api/financial-plans/{planId}/dashboard", planId))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").exists());
    }

    // =================== 错误路径 ===================

    /**
     * 错误：归档后再编辑 → FP_PLAN_ALREADY_ARCHIVED（INV-7）。
     *
     * <p>计划归档后，service.updatePlan() 优先检查 status，无论 version 是否正确
     * 均抛出 FP_PLAN_ALREADY_ARCHIVED。
     */
    @Test
    void error_editArchivedPlan_failsWithArchiveError() throws Exception {
        Long planId = createPlan("归档锁定测试", "SAVINGS", null, "YEAR", 2026);
        int version = getPlanVersion(planId); // = 0

        // 归档计划（DELETE 接口）
        Map<String, Object> archiveReq = new HashMap<>();
        archiveReq.put("version", version);
        mockMvc.perform(delete("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(jsonPath("$.code").value("0"));

        // 归档后尝试更新 → 应返回 FP_PLAN_ALREADY_ARCHIVED
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("planName", "归档后改名");
        updateReq.put("timeRangeType", "YEAR");
        updateReq.put("fiscalYear", 2026);
        updateReq.put("version", version + 1); // 归档后 version 已递增
        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_PLAN_ALREADY_ARCHIVED"));
    }

    /**
     * 错误：未买入直接卖出 → FP_SELL_BEFORE_BUY（INV-4）。
     *
     * <p>批次处于 PENDING_BUY 状态时调用 recordSell，service 检测阶段非法并抛出错误。
     */
    @Test
    void error_sellBeforeBuy_failsWithSellBeforeBuyError() throws Exception {
        Long planId = createPlan("先卖后买测试", "STOCK", "EQUITY", "YEAR", 2026);
        Long assetId = saveFirstAsset(planId, "AMZN", "亚马逊", "180", "240", "200");
        Long batchId = createBatch(planId, assetId, "100");

        // 未买入直接卖出（批次在 PENDING_BUY 状态）
        Map<String, Object> sellBody = new HashMap<>();
        sellBody.put("tradeDate", "2026-03-01");
        sellBody.put("actualSellPrice", "230.0");
        sellBody.put("quantity", "100");
        sellBody.put("fee", "60.0");
        sellBody.put("version", 0);

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations/{batchId}/sell", planId, batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_SELL_BEFORE_BUY"));
    }

    /**
     * 错误：批次申请数量超过标的计划数量 → FP_REALIZATION_QTY_EXCEEDED（INV-3）。
     *
     * <p>planQuantity=100，批次申请 quantity=101，触发超量校验。
     */
    @Test
    void error_batchQtyExceedsPlanQty_failsWithQtyExceeded() throws Exception {
        Long planId = createPlan("数量超标测试", "STOCK", "EQUITY", "YEAR", 2026);
        // planQuantity=100
        Long assetId = saveFirstAsset(planId, "META", "Meta", "300", "400", "100");

        // 申请 quantity=101，超过 planQuantity=100
        Map<String, Object> batchReq = new HashMap<>();
        batchReq.put("assetId", assetId);
        batchReq.put("batchName", "超标批次");
        batchReq.put("quantity", "101");

        mockMvc.perform(post("/api/financial-plans/{planId}/realizations", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_REALIZATION_QTY_EXCEEDED"));
    }

    /**
     * 错误：使用过期版本号更新计划 → FP_VERSION_CONFLICT。
     *
     * <p>第一次更新成功后 version=1，再用 version=0 更新触发乐观锁冲突。
     */
    @Test
    void error_staleVersionUpdate_failsWithVersionConflict() throws Exception {
        Long planId = createPlan("版本冲突测试", "SAVINGS", null, "YEAR", 2026);
        int version = getPlanVersion(planId); // = 0

        // 第一次更新成功，version 变为 1
        Map<String, Object> firstUpdate = new HashMap<>();
        firstUpdate.put("planName", "第一次更新");
        firstUpdate.put("timeRangeType", "YEAR");
        firstUpdate.put("fiscalYear", 2026);
        firstUpdate.put("version", version);
        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpdate)))
                .andExpect(jsonPath("$.code").value("0"));

        // 第二次用过期 version=0 更新 → 版本冲突
        Map<String, Object> staleUpdate = new HashMap<>();
        staleUpdate.put("planName", "过期版本更新");
        staleUpdate.put("timeRangeType", "YEAR");
        staleUpdate.put("fiscalYear", 2026);
        staleUpdate.put("version", version); // 仍是旧 version=0
        mockMvc.perform(put("/api/financial-plans/{planId}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_VERSION_CONFLICT"));
    }

    /**
     * 错误：查询不存在的计划 → FP_PLAN_NOT_FOUND。
     */
    @Test
    void error_planNotFound_failsWithNotFoundError() throws Exception {
        mockMvc.perform(get("/api/financial-plans/{planId}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FP_PLAN_NOT_FOUND"));
    }

    // =================== 私有辅助方法 ===================

    /**
     * 创建理财计划，返回 planId。
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
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        return asLong(data.get("planId"));
    }

    /**
     * 保存单个标的，返回 assetId。
     * 响应结构：$.data.items[0].assetId
     */
    private Long saveFirstAsset(Long planId, String assetCode, String assetName,
                                String buyPrice, String sellPrice, String qty) throws Exception {
        List<Map<String, Object>> items = buildAssetItems(assetCode, assetName, buyPrice, sellPrice, qty);
        Map<String, Object> req = new HashMap<>();
        req.put("items", items);

        String resp = mockMvc.perform(post("/api/financial-plans/{planId}/assets/save", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        List<?> savedItems = (List<?>) data.get("items");
        Map<?, ?> firstItem = (Map<?, ?>) savedItems.get(0);
        return asLong(firstItem.get("assetId"));
    }

    /**
     * 从 API-9 详情响应中解析指定标的的 version 字段。
     * 用于调用 API-5 时传入正确的乐观锁版本号。
     */
    private int getAssetVersion(Long planId, Long assetId) throws Exception {
        String resp = mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        List<?> assets = (List<?>) data.get("assets");
        for (Object asset : assets) {
            Map<?, ?> a = (Map<?, ?>) asset;
                        if (asLong(a.get("assetId")).equals(assetId)) {
                                return asInt(a.get("version"));
            }
        }
        throw new IllegalStateException("Asset not found in plan detail: assetId=" + assetId);
    }

    /**
     * 从 API-9 详情响应中解析计划的 version 字段。
     * 用于调用 API-3 / DELETE 时传入正确的乐观锁版本号。
     */
    private int getPlanVersion(Long planId) throws Exception {
        String resp = mockMvc.perform(get("/api/financial-plans/{planId}", planId))
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        Map<?, ?> plan = (Map<?, ?>) data.get("plan");
        return asInt(plan.get("version"));
    }

    /**
     * 构建单个标的的请求列表（STOCK / EQUITY 类型）。
     */
    private List<Map<String, Object>> buildAssetItems(String assetCode, String assetName,
                                                      String buyPrice, String sellPrice, String qty) {
        Map<String, Object> item = new HashMap<>();
        item.put("assetType", "STOCK");
        item.put("assetCode", assetCode);
        item.put("assetName", assetName);
        item.put("stockSubType", "EQUITY");
        item.put("planBuyPrice", buyPrice);
        item.put("planSellPrice", sellPrice);
        item.put("planQuantity", qty);
        item.put("currency", "USD");
        item.put("sequenceNo", 1);
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    /**
     * 创建兑现批次，返回 batchId。
     */
    private Long createBatch(Long planId, Long assetId, String qty) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("assetId", assetId);
        body.put("batchName", "测试批次");
        body.put("quantity", qty);

        String resp = mockMvc.perform(post("/api/financial-plans/{planId}/realizations", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> result = objectMapper.readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        return asLong(data.get("batchId"));
    }

    /**
     * 登记买入操作（API-7）。
     *
     * @param version 批次当前乐观锁版本号（新建批次为 0）
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

        private Long asLong(Object value) {
                if (value == null) {
                        throw new IllegalStateException("Expected non-null numeric value");
                }
                if (value instanceof Number) {
                        return ((Number) value).longValue();
                }
                return Long.parseLong(String.valueOf(value));
        }

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
