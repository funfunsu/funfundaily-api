# 开放任务接口 + MCP 服务 — 改动与决策清单

> 需求：开放 `getToDoTaskList` / `getNextTodoTask`（按创建时间排序）/ `checkInTask` 三个接口
> 及对应 MCP 服务，采用 Bearer Token 鉴权，token 与 groupId 关联做鉴权与数据隔离，
> 过滤条件支持 groupId / parentId / userId，返回任务的标题与内容。

## 1. 做了什么（一句话）

新增一套**对外开放**、与小程序 JWT 链路完全隔离的任务接口（`/openapi/task/**`），
用 **Bearer Token ↔ groupId** 鉴权并按 groupId 做数据隔离；再用一个独立的 **Node/TS MCP 服务**
把这三个接口包装成 MCP 工具。

## 2. 接口契约

鉴权：请求头 `Authorization: Bearer <token>`。token 在后端绑定一个 `groupId`，
所有数据访问被限定在该群组内（数据隔离）。鉴权失败返回 HTTP 401 + `{"code":"4010001"}`。

| 方法 & 路径 | 功能 | 过滤参数 | 返回 |
| --- | --- | --- | --- |
| `GET /openapi/task/list` | getToDoTaskList | `groupId?`(校验) `userId?` `parentId?` | 任务数组，按 createTime 升序 |
| `GET /openapi/task/next` | getNextTodoTask | `groupId?`(校验) `userId?` `parentId?` | 最早创建且当前周期未完成的任务；无则空 |
| `POST /openapi/task/checkin` | checkInTask | body: `taskId`(必填) `userId?` `taskTime?` `groupId?`(校验) | 打卡记录 ID |

- `groupId` 始终以 token 绑定值为准；若调用方显式传 `groupId` 且与 token 不一致 → `4030000`（越权拒绝）。
- 单条任务返回字段：`id, title, content, groupId, userId, parentId, taskType, repeatType, totalCount, completedCount, completed, createTime`。
  - `title` = `schedule_item.item_title`，`content` = `schedule_item.item_desc`。
- 统一响应信封 `{code, message, data}`；`code="0"` 为成功。注意 Long 被后端序列化为字符串。

## 3. 鉴权与数据隔离设计

- 新增独立拦截器 `OpenApiAuthInterceptor`，只作用于 `/openapi/**`，与小程序 `AuthInterceptor`（`/api/**`，JWT）互不影响。
- token → groupId 的映射走配置 `openapi.tokens`（`OpenApiTokenConfig`，`@ConfigurationProperties`）。
  - 选配置而非建表：零 schema 改动、易轮换；如需可平滑迁移到数据库表。
- 拦截器把 groupId 写入 `OpenApiContext`（ThreadLocal），请求结束 `clear()`。
- 所有查询/打卡都强制带 groupId；`checkInTask` 额外复核任务实体的 groupId 归属，跨群组打卡 → `4030000`。

## 4. 数据模型映射

- 「任务」即 `schedule_item` 中 `item_type = "task"` 的记录（复用既有模型，不新建表）。
- 任务子类型（Habit/Todo）、目标次数在 `extra` JSON 里：`extra.taskType`、`extra.totalCount`。
- **完成进度**复用既有打卡逻辑：按 `taskKey`（`taskId:周期键`）统计 `checkin_record` 条数，
  与小程序端打卡口径一致。`completed` 判定：设了 totalCount 则达标即完成，否则打卡 ≥1 次即完成。
- `getNextTodoTask`：按 createTime 升序，取第一个 `completed != true` 的任务。
- 列表只取 `close_status <> CLOSE`（未停止关注）的任务。

## 5. MCP 服务

- 位置：`backend/mcp-server/`（独立 Node/TS，stdio 传输，`@modelcontextprotocol/sdk`）。
- 为什么独立而非内嵌 Spring：后端是 **Spring Boot 2.7 / Java 11**，Spring AI 的 MCP starter 需 Boot 3 / Java 17，不兼容；
  独立 MCP 服务解耦、可单独部署，调用上面的 REST 接口即可。
- 工具：`get_todo_task_list` / `get_next_todo_task` / `check_in_task`。
- 配置：`FUNFUN_OPENAPI_TOKEN`（必填）、`FUNFUN_API_BASE_URL`（默认 `http://localhost:8080`）。
- 详见 `backend/mcp-server/README.md`。

## 6. 令牌配置（按环境）

- `application.yml`：`openapi.tokens: []` + 用法注释。
- `application-dev.yml`：内置测试令牌 `dev-openapi-token-group1` → `groupId=1`。
- `application-prod.yml`：从环境变量注入（`OPENAPI_TOKEN_GROUP1` 等），默认 `enabled:false`。

## 7. 验证

- 集成测试 `OpenTaskApiIntegrationTest`（7 个用例，全过）：缺 token/错 token→401、按群隔离、
  按 createTime 升序、parentId/userId 过滤、next 取最早未完成、打卡后置为已完成、跨群打卡拒绝、groupId 不匹配拒绝。
- 真机 HTTP E2E（dev profile，H2，端口 8089）：用 JWT 建任务 → 开放接口三连 → 打卡后 `completed=true`、
  `next` 返回空、`?groupId=2` 被拒（4030000）。
- MCP 服务 E2E：stdio JSON-RPC `initialize`/`tools/list`/`tools/call` 跑通，三个工具对实跑后端成功。
- ⚠️ 既有不相关失败：`FinancialPlanServiceImplTest.createPlanWithYearWindowShouldDeriveNaturalYearDates`
  （expected DRAFT but ACTIVE）——与本次改动无关，是日期敏感的既有用例（今天 2026-05-23 落在自然年窗口内）。

## 8. 改动文件清单

**新增（本次功能，命名空间清晰、互不污染）**
- `schedule-service/.../config/OpenApiTokenConfig.java`
- `schedule-service/.../context/OpenApiContext.java`
- `schedule-service/.../dto/TodoTaskDTO.java`
- `schedule-service/.../service/TodoTaskService.java`
- `schedule-service/.../service/impl/TodoTaskServiceImpl.java`
- `schedule-web/.../interceptor/OpenApiAuthInterceptor.java`
- `schedule-web/.../controller/openapi/OpenTaskController.java`
- `schedule-web/.../dto/openapi/OpenCheckinRequest.java`
- `schedule-start/src/test/java/.../openapi/OpenTaskApiIntegrationTest.java`
- `backend/mcp-server/**`（MCP 服务）

**修改（本次新增的小改动）**
- `schedule-service/.../repository/ScheduleItemRepository.java`：新增 `findOpenTasksForOpenApi(...)` 查询。
- `schedule-web/.../config/WebConfig.java`：注册 `OpenApiAuthInterceptor`（`/openapi/**`）。
- `schedule-start/src/main/resources/application.yml | application-dev.yml | application-prod.yml`：`openapi.tokens` 配置。

> 注意：`ScheduleItemRepository.java` 同时含**邀请函在途（未提交）**的改动（`findByGroupIdAndCreateBy...` 等、
> `findOverlapping*` 的 closeStatus 过滤）；这些不是本次内容。提交时建议把本次「开放接口」相关 hunk
> 与邀请函在途改动分开。

## 9. 假设与决策（待 review）

1. **「待办任务」= `item_type="task"` 的 schedule_item**。未把范围窄化到 `taskType=Todo`，
   因为「待办」更贴近「所有任务项」，且 Habit/Todo 都通过打卡完成；如需只看 Todo 可加过滤。
2. **令牌走配置而非数据库表**（零 schema 改动、易轮换；可后续迁库）。
3. **`groupId` 由 token 决定**；请求里传的 groupId 仅作一致性校验，不能借此跨群。
4. **完成进度按任务归属成员（item.userId）统计**当前周期打卡数，与小程序口径一致。
5. **list/next 用 GET（查询参数）、checkin 用 POST（JSON body）**——读操作便于 curl/MCP 直接调用。
6. **MCP 服务独立部署**（Boot 2.7/Java 11 与 Spring AI MCP 不兼容）。
7. **路径用 `/openapi/**`**（独立命名空间），不复用 `/api/public/**`（那是「免鉴权」语义，本接口是「令牌鉴权」）。
8. **未自动提交**：因 `ScheduleItemRepository.java` 等与邀请函在途改动交织，且本环境无法交互式分块暂存，
   故保留工作区改动交由你 review 后再提交（符合「等我回来一并 review」）。
