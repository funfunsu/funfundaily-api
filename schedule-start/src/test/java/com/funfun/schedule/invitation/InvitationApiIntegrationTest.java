package com.funfun.schedule.invitation;

import com.fasterxml.jackson.databind.JsonNode;
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

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 邀请函 API 集成测试（schedule_item 存储改造）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>创建「发出的邀请」（save）并能在主页列表 sent 中看到</li>
 *   <li>另一用户「收下邀请」（accept）生成 received 记录，parentId 指向原邀请</li>
 *   <li>幂等：重复 accept 返回同一条记录</li>
 *   <li>原邀请时间/地点变更后，received 记录跟随变更并追加 updateScope 变更记录</li>
 * </ul>
 *
 * <p>H2 dev profile，{@code @MockBean LoginCheckUtil} 控制当前登录用户，{@code @Transactional} 回滚隔离。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class InvitationApiIntegrationTest {

    @MockBean
    private LoginCheckUtil loginCheckUtil;

    @MockBean
    private UserVipService userVipService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long GROUP_ID = 1L;
    private static final Long CREATOR = 1L;   // 与 data.sql 种子用户对齐
    private static final Long INVITEE = 2L;   // 受邀人

    @BeforeEach
    void setUp() {
        loginAs(CREATOR);
        when(userVipService.getUserVip(any())).thenReturn(Optional.empty());
    }

    private void loginAs(Long userId) {
        when(loginCheckUtil.checkLoginAndGetUserId(any(HttpServletRequest.class))).thenReturn(userId);
    }

    private JsonNode postJson(String url, Map<String, Object> body) throws Exception {
        String resp = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp);
    }

    private Long createInvitation(String title, String start, String end, String address) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("groupId", GROUP_ID);
        body.put("title", title);
        body.put("startTime", start);
        body.put("endTime", end);
        body.put("address", address);
        body.put("body", "欢迎参加");
        body.put("cardStyle", "classic");
        body.put("signature", "小明一家");
        JsonNode data = postJson("/api/invitation/save", body).get("data");
        assertEquals("sent", data.get("direction").asText());
        return data.get("id").asLong();
    }

    @Test
    void create_and_listShowsSent() throws Exception {
        Long id = createInvitation("生日派对", "2026-06-01 18:00", "2026-06-01 20:00", "上海某餐厅");

        String resp = mockMvc.perform(get("/api/invitation/list").param("groupId", String.valueOf(GROUP_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode list = objectMapper.readTree(resp).get("data");
        assertTrue(list.isArray());
        boolean found = false;
        for (JsonNode n : list) {
            if (n.get("id").asLong() == id && "sent".equals(n.get("direction").asText())) {
                found = true;
                assertEquals("2026-06-01 18:00", n.get("startTime").asText());
            }
        }
        assertTrue(found, "sent 列表应包含刚创建的邀请函");
    }

    @Test
    void accept_createsReceivedLinkedToParent_andIsIdempotent() throws Exception {
        Long id = createInvitation("满月酒", "2026-07-10 11:00", "2026-07-10 13:00", "杭州某酒店");

        // 受邀人收下
        loginAs(INVITEE);
        Map<String, Object> acceptBody = new HashMap<>();
        acceptBody.put("invitationId", id);
        acceptBody.put("recipientName", "张阿姨一家");
        JsonNode recv = postJson("/api/invitation/accept", acceptBody).get("data");
        long recvId = recv.get("id").asLong();
        assertEquals("received", recv.get("direction").asText());
        assertEquals(id, recv.get("parentId").asLong());
        assertEquals("2026-07-10 11:00", recv.get("startTime").asText());

        // 幂等：再次收下返回同一条
        JsonNode recv2 = postJson("/api/invitation/accept", acceptBody).get("data");
        assertEquals(recvId, recv2.get("id").asLong());

        // 受邀人主页能看到「收到的」
        String listResp = mockMvc.perform(get("/api/invitation/list").param("groupId", String.valueOf(GROUP_ID)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode list = objectMapper.readTree(listResp).get("data");
        boolean found = false;
        for (JsonNode n : list) {
            if (n.get("id").asLong() == recvId) {
                found = true;
                assertEquals("received", n.get("direction").asText());
            }
        }
        assertTrue(found, "受邀人 received 列表应包含收下的邀请函");
    }

    @Test
    void receivedInvitation_showsInScheduleList() throws Exception {
        Long id = createInvitation("音乐会", "2026-09-15 18:00", "2026-09-15 20:00", "音乐厅");

        // 受邀人收下
        loginAs(INVITEE);
        Map<String, Object> acceptBody = new HashMap<>();
        acceptBody.put("invitationId", id);
        acceptBody.put("recipientName", "小红");
        postJson("/api/invitation/accept", acceptBody);

        // 受邀人查询本周日程表（不传 groupId/targetUserId → 默认查自己，scheduleItemType 默认 schedule）
        Map<String, Object> query = new HashMap<>();
        query.put("fromDate", "2026-09-14");
        query.put("toDate", "2026-09-16");
        query.put("scheduleItemType", "schedule");
        JsonNode list = postJson("/api/schedule/list", query).get("data");

        boolean found = false;
        for (JsonNode day : list) {
            if (!"2026-09-15".equals(day.get("date").asText())) {
                continue;
            }
            for (JsonNode s : day.get("schedules")) {
                if ("invRecv".equals(s.get("itemType").asText()) && "音乐会".equals(s.get("itemTitle").asText())) {
                    found = true;
                }
            }
        }
        assertTrue(found, "日程表应包含收下的邀请（invRecv）事件");
    }

    @Test
    void editTimeAndLocation_cascadesToReceived_withChangeLog() throws Exception {
        Long id = createInvitation("家宴", "2026-08-01 17:00", "2026-08-01 19:00", "旧地址");

        // 受邀人收下
        loginAs(INVITEE);
        Map<String, Object> acceptBody = new HashMap<>();
        acceptBody.put("invitationId", id);
        acceptBody.put("recipientName", "李叔叔");
        long recvId = postJson("/api/invitation/accept", acceptBody).get("data").get("id").asLong();

        // 创建人改时间 + 地点
        loginAs(CREATOR);
        Map<String, Object> update = new HashMap<>();
        update.put("id", id);
        update.put("groupId", GROUP_ID);
        update.put("title", "家宴");
        update.put("startTime", "2026-08-02 18:30");
        update.put("endTime", "2026-08-02 20:30");
        update.put("address", "新地址·浦东");
        update.put("body", "欢迎参加");
        update.put("cardStyle", "classic");
        update.put("signature", "小明一家");
        postJson("/api/invitation/save", update);

        // 受邀人的收到记录应已跟随变更，且带变更记录
        loginAs(INVITEE);
        String listResp = mockMvc.perform(get("/api/invitation/list").param("groupId", String.valueOf(GROUP_ID)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode list = objectMapper.readTree(listResp).get("data");
        JsonNode recv = null;
        for (JsonNode n : list) {
            if (n.get("id").asLong() == recvId) {
                recv = n;
            }
        }
        assertTrue(recv != null, "应能查到受邀人的收到记录");
        assertEquals("2026-08-02 18:30", recv.get("startTime").asText());
        assertEquals("2026-08-02 20:30", recv.get("endTime").asText());
        assertEquals("新地址·浦东", recv.get("address").asText());
        JsonNode changes = recv.get("changes");
        assertTrue(changes != null && changes.isArray() && changes.size() >= 1,
                "收到记录应至少有一条变更记录");
        assertTrue(changes.get(0).get("summary").asText().length() > 0, "变更记录应有摘要");
    }
}
