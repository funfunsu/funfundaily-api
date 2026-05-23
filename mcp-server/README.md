# funfun 待办任务 MCP 服务

通过 [MCP](https://modelcontextprotocol.io)（stdio 传输）把后端「开放任务接口」包装成可被
Claude / 其它 MCP 客户端调用的工具。底层调用后端 `/openapi/task/**`，使用 **Bearer Token** 鉴权，
token 在后端绑定 `groupId`，因此所有数据访问都被隔离在该群组内。

## 工具

| 工具名 | 说明 | 入参 | 后端接口 |
| --- | --- | --- | --- |
| `get_todo_task_list` | 获取待办任务列表（按创建时间升序） | `userId?`、`parentId?` | `GET /openapi/task/list` |
| `get_next_todo_task` | 下一个（最早创建且未完成）待办任务 | `userId?`、`parentId?` | `GET /openapi/task/next` |
| `check_in_task` | 对任务打卡 / 完成 | `taskId`、`userId?`、`taskTime?` | `POST /openapi/task/checkin` |

> `groupId` 不在工具参数里——它由 token 决定，避免越权访问其它群组。
> 返回内容含任务的**标题（title）**与**内容（content）**，以及完成进度等元信息。

## 环境变量

| 变量 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `FUNFUN_OPENAPI_TOKEN` | 是 | — | 后端开放接口访问令牌（绑定某 groupId） |
| `FUNFUN_API_BASE_URL` | 否 | `http://localhost:8080` | 后端基址 |

## 构建与运行

```bash
cd backend/mcp-server
npm install        # 若全局 npm 缓存异常，可加 --cache ./.npm-cache
npm run build      # tsc -> dist/index.js
FUNFUN_OPENAPI_TOKEN=xxxx FUNFUN_API_BASE_URL=http://localhost:8080 node dist/index.js
```

## 在 MCP 客户端中注册

以 Claude Desktop 的 `claude_desktop_config.json` 为例：

```json
{
  "mcpServers": {
    "funfun-task": {
      "command": "node",
      "args": ["/绝对路径/backend/mcp-server/dist/index.js"],
      "env": {
        "FUNFUN_API_BASE_URL": "http://localhost:8080",
        "FUNFUN_OPENAPI_TOKEN": "<你的令牌>"
      }
    }
  }
}
```

## 后端令牌配置

令牌在后端 `application*.yml` 的 `openapi.tokens` 维护，每个 token 绑定一个 groupId：

```yaml
openapi:
  tokens:
    - token: <随机长字符串>
      groupId: 1
      name: 家庭A
      enabled: true
```

dev 环境已内置测试令牌 `dev-openapi-token-group1`（绑定 `groupId=1`）。
生产环境请用环境变量注入，切勿把真实令牌提交进仓库。
