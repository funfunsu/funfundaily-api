package com.funfun.schedule.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.repository.ScheduleItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 开放接口（OpenAPI / MCP）任务能力集成测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>Bearer Token 鉴权（缺失 / 错误令牌 → 401）</li>
 *   <li>基于 groupId 的数据隔离（令牌绑定 group=1，看不到 group=2 的任务）</li>
 *   <li>getToDoTaskList：按创建时间升序、parentId / userId 过滤</li>
 *   <li>getNextTodoTask：返回创建时间最早、当前周期未完成的任务</li>
 *   <li>checkInTask：打卡成功后该任务标记为已完成；跨群组任务打卡被拒绝</li>
 * </ul>
 *
 * <p>dev profile（H2 + data.sql 给 group 1 种子令牌 fun_group.open_api_token=dev-openapi-token-group1），
 * {@code @Transactional} 回滚隔离。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class OpenTaskApiIntegrationTest {

    private static final Long GROUP_ID = 1L;        // 与 dev 令牌绑定群组一致
    private static final Long OTHER_GROUP_ID = 2L;  // 令牌无权访问
    private static final Long USER_ID = 1L;
    private static final String TOKEN = "Bearer dev-openapi-token-group1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleItemRepository scheduleItemRepository;

    @BeforeEach
    void setUp() {
        scheduleItemRepository.deleteAll();
    }

    @Test
    void list_requiresToken() throws Exception {
        mockMvc.perform(get("/openapi/task/list"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/openapi/task/list").header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsTasksSortedByCreateTime_isolatedByGroup() throws Exception {
        Long t1 = createTask("最早的任务", "内容1", GROUP_ID, USER_ID, 0L, daysAgo(3), 1);
        Long t2 = createTask("较晚的任务", "内容2", GROUP_ID, USER_ID, 0L, daysAgo(1), 1);
        createTask("别的群组任务", "内容X", OTHER_GROUP_ID, USER_ID, 0L, daysAgo(2), 1);

        MvcResult result = mockMvc.perform(get("/openapi/task/list").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode data = readData(result);
        assertEquals(2, data.size(), "只应看到本群组的任务");
        assertEquals(String.valueOf(t1), data.get(0).get("id").asText(), "应按创建时间升序，最早的在前");
        assertEquals("最早的任务", data.get(0).get("title").asText());
        assertEquals("内容1", data.get(0).get("content").asText());
        assertEquals(String.valueOf(t2), data.get(1).get("id").asText());
    }

    @Test
    void list_filtersByParentIdAndUserId() throws Exception {
        createTask("父=0 用户=1", "c", GROUP_ID, 1L, 0L, daysAgo(3), 1);
        Long child = createTask("父=100 用户=1", "c", GROUP_ID, 1L, 100L, daysAgo(2), 1);
        createTask("父=0 用户=2", "c", GROUP_ID, 2L, 0L, daysAgo(1), 1);

        MvcResult byParent = mockMvc.perform(get("/openapi/task/list")
                        .param("parentId", "100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode byParentData = readData(byParent);
        assertEquals(1, byParentData.size());
        assertEquals(String.valueOf(child), byParentData.get(0).get("id").asText());

        MvcResult byUser = mockMvc.perform(get("/openapi/task/list")
                        .param("userId", "2")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1, readData(byUser).size());
    }

    @Test
    void list_rejectsMismatchedGroupId() throws Exception {
        mockMvc.perform(get("/openapi/task/list")
                        .param("groupId", String.valueOf(OTHER_GROUP_ID))
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("4030000")); // NOT_ALLOWED
    }

    @Test
    void next_returnsEarliestIncompleteTask() throws Exception {
        Long a = createTask("任务A", "ca", GROUP_ID, USER_ID, 0L, daysAgo(3), 1);
        Long b = createTask("任务B", "cb", GROUP_ID, USER_ID, 0L, daysAgo(1), 1);

        // 初始：最早的 A 未完成 -> next = A
        MvcResult first = mockMvc.perform(get("/openapi/task/next").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(String.valueOf(a), readData(first).get("id").asText());

        // 完成 A（totalCount=1，打卡一次即完成）
        checkin(a, null);

        // next 应跳到 B
        MvcResult second = mockMvc.perform(get("/openapi/task/next").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(String.valueOf(b), readData(second).get("id").asText());
    }

    @Test
    void checkin_succeedsAndMarksCompleted() throws Exception {
        Long taskId = createTask("打卡任务", "c", GROUP_ID, USER_ID, 0L, daysAgo(1), 1);

        checkin(taskId, null);

        MvcResult listResult = mockMvc.perform(get("/openapi/task/list").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode item = readData(listResult).get(0);
        assertEquals(1, item.get("completedCount").asInt());
        assertTrue(item.get("completed").asBoolean(), "打卡后应标记为已完成");
    }

    @Test
    void checkin_rejectsCrossGroupTask() throws Exception {
        Long otherTask = createTask("别群组任务", "c", OTHER_GROUP_ID, USER_ID, 0L, daysAgo(1), 1);

        Map<String, Object> body = new HashMap<>();
        body.put("taskId", otherTask);

        mockMvc.perform(post("/openapi/task/checkin")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("4030000")); // NOT_ALLOWED
    }

    // ----- helpers -----

    private void checkin(Long taskId, LocalDateTime taskTime) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        if (taskTime != null) {
            body.put("taskTime", taskTime.toString());
        }
        mockMvc.perform(post("/openapi/task/checkin")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    private JsonNode readData(MvcResult result) throws Exception {
        // 显式按 UTF-8 读取，避免 MockHttpServletResponse 默认 ISO-8859-1 把中文读乱码。
        String json = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(json);
        return root.get("data");
    }

    private Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - days * 24L * 3600_000L);
    }

    private Long createTask(String title, String content, Long groupId, Long userId,
                            Long parentId, Date createTime, int totalCount) {
        ScheduleItem item = new ScheduleItem();
        item.setItemTitle(title);
        item.setItemDesc(content);
        item.setItemType("task");
        item.setRepeatType("daily");
        item.setRepeatStartDay(java.time.LocalDate.now().minusDays(30));
        item.setRepeatEndDay(java.time.LocalDate.now().plusDays(30));
        item.setStartTime(LocalDateTime.now().minusDays(30));
        item.setEndTime(LocalDateTime.now().plusDays(30));
        item.setUserId(userId);
        item.setGroupId(groupId);
        item.setParentId(parentId);
        item.setCloseStatus(CloseStatus.OPEN);
        item.setExtra("{\"taskType\":\"Todo\",\"totalCount\":" + totalCount + ",\"score\":0}");
        item.setCreateBy(userId);
        item.setUpdateBy(userId);
        item.setCreateTime(createTime);
        item.setUpdateTime(createTime);
        return scheduleItemRepository.save(item).getId();
    }
}
