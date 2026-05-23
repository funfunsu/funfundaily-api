#!/usr/bin/env node
/**
 * funfun 待办任务 MCP 服务
 *
 * 通过 MCP（stdio 传输）暴露三个工具，底层调用后端开放接口（/openapi/task/**）：
 *   - get_todo_task_list  -> GET  /openapi/task/list
 *   - get_next_todo_task  -> GET  /openapi/task/next
 *   - check_in_task       -> POST /openapi/task/checkin
 *
 * 鉴权：所有请求携带 `Authorization: Bearer <token>`；token 在后端绑定 groupId，
 * 因此 MCP 侧无需（也无法）跨群组访问——数据隔离由后端令牌保证。
 *
 * 环境变量：
 *   FUNFUN_API_BASE_URL  后端基址，默认 http://localhost:8080
 *   FUNFUN_OPENAPI_TOKEN  访问令牌（必填）
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const BASE_URL = (process.env.FUNFUN_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");
const TOKEN = process.env.FUNFUN_OPENAPI_TOKEN ?? "";

if (!TOKEN) {
  // 写到 stderr，不污染 stdio 上的 MCP 协议流。
  console.error("[funfun-task-mcp] 缺少环境变量 FUNFUN_OPENAPI_TOKEN，无法鉴权。");
  process.exit(1);
}

interface CommonResponse<T> {
  code: string;
  message: string;
  data: T;
}

/** 后端返回的待办任务视图（注意：Long 字段被后端序列化为字符串）。 */
interface TodoTask {
  id: string;
  title: string;
  content?: string;
  groupId?: string;
  userId?: string;
  parentId?: string;
  taskType?: string;
  repeatType?: string;
  totalCount?: number;
  completedCount?: number;
  completed?: boolean;
  createTime?: string;
}

/** 统一发起后端请求并解开 CommonResponse 信封；失败抛出可读错误。 */
async function callApi<T>(
  path: string,
  options: { method?: string; query?: Record<string, unknown>; body?: unknown } = {}
): Promise<T> {
  const url = new URL(BASE_URL + path);
  if (options.query) {
    for (const [k, v] of Object.entries(options.query)) {
      if (v !== undefined && v !== null) {
        url.searchParams.set(k, String(v));
      }
    }
  }

  const res = await fetch(url, {
    method: options.method ?? "GET",
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      "Content-Type": "application/json",
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (res.status === 401) {
    throw new Error("鉴权失败（401）：访问令牌缺失、无效或已禁用。");
  }

  const text = await res.text();
  let parsed: CommonResponse<T>;
  try {
    parsed = JSON.parse(text);
  } catch {
    throw new Error(`后端返回非 JSON（HTTP ${res.status}）：${text.slice(0, 500)}`);
  }

  if (parsed.code !== "0") {
    throw new Error(`接口返回错误 code=${parsed.code}：${parsed.message}`);
  }
  return parsed.data;
}

function formatTask(t: TodoTask): string {
  const parts = [
    `#${t.id} ${t.title ?? "(无标题)"}`,
    t.content ? `内容：${t.content}` : "内容：(空)",
  ];
  if (t.completedCount !== undefined || t.totalCount !== undefined) {
    parts.push(`进度：${t.completedCount ?? 0}/${t.totalCount ?? "?"}${t.completed ? "（已完成）" : ""}`);
  }
  if (t.userId) parts.push(`成员：${t.userId}`);
  if (t.parentId && t.parentId !== "0") parts.push(`父任务：${t.parentId}`);
  return parts.join(" | ");
}

const server = new McpServer({
  name: "funfun-task-mcp-server",
  version: "0.1.0",
});

server.registerTool(
  "get_todo_task_list",
  {
    title: "获取待办任务列表",
    description:
      "获取当前令牌所属群组的待办任务列表（按创建时间升序）。可选按 userId、parentId 过滤。返回每个任务的标题与内容。",
    inputSchema: {
      userId: z.number().int().optional().describe("按归属成员过滤（可选）"),
      parentId: z.number().int().optional().describe("按父任务 ID 过滤（可选）"),
    },
  },
  async ({ userId, parentId }) => {
    const tasks = await callApi<TodoTask[]>("/openapi/task/list", {
      method: "GET",
      query: { userId, parentId },
    });
    const summary =
      tasks.length === 0
        ? "没有匹配的待办任务。"
        : `共 ${tasks.length} 个待办任务：\n` + tasks.map(formatTask).join("\n");
    return {
      content: [
        { type: "text", text: summary },
        { type: "text", text: JSON.stringify(tasks) },
      ],
    };
  }
);

server.registerTool(
  "get_next_todo_task",
  {
    title: "获取下一个待办任务",
    description:
      "按创建时间排序，返回下一个（最早创建且当前周期未完成的）待办任务。可选按 userId、parentId 过滤。无未完成任务时返回提示。",
    inputSchema: {
      userId: z.number().int().optional().describe("按归属成员过滤（可选）"),
      parentId: z.number().int().optional().describe("按父任务 ID 过滤（可选）"),
    },
  },
  async ({ userId, parentId }) => {
    const task = await callApi<TodoTask | null>("/openapi/task/next", {
      method: "GET",
      query: { userId, parentId },
    });
    if (!task) {
      return { content: [{ type: "text", text: "当前没有未完成的待办任务。" }] };
    }
    return {
      content: [
        { type: "text", text: `下一个待办任务：\n${formatTask(task)}` },
        { type: "text", text: JSON.stringify(task) },
      ],
    };
  }
);

server.registerTool(
  "check_in_task",
  {
    title: "完成 / 打卡任务",
    description:
      "对指定任务执行一次打卡 / 完成。taskId 必填；userId 可选（默认任务归属成员）；taskTime 可选（ISO 时间，默认当天）。返回打卡记录 ID。",
    inputSchema: {
      taskId: z.number().int().describe("任务 ID（必填）"),
      userId: z.number().int().optional().describe("打卡成员 ID（可选，默认任务归属成员）"),
      taskTime: z
        .string()
        .optional()
        .describe("打卡所属时间，ISO 格式如 2026-05-23T09:00:00（可选，默认当天）"),
    },
  },
  async ({ taskId, userId, taskTime }) => {
    const recordId = await callApi<string>("/openapi/task/checkin", {
      method: "POST",
      body: { taskId, userId, taskTime },
    });
    return {
      content: [{ type: "text", text: `打卡成功，记录 ID：${recordId}` }],
    };
  }
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("[funfun-task-mcp] 已启动（stdio）。base=" + BASE_URL);
}

main().catch((err) => {
  console.error("[funfun-task-mcp] 启动失败：", err);
  process.exit(1);
});
