#!/usr/bin/env bash
# fun_harness_start.sh — VS Code「启动服务」入口
#
# 多模块 Spring Boot 工程（聚合 pom，packaging=pom），子模块：
#   - schedule-service  : 业务/数据访问层（含 spring-boot starter 依赖，但无 @SpringBootApplication）
#   - schedule-web      : web/controller 层（同上）
#   - schedule-start  ✅ : 唯一带 @SpringBootApplication + spring-boot-maven-plugin 的可启动模块
#
# 默认启动 schedule-start（主服务）。如需切换到其他可启动模块，改 LAUNCH_MODULE 即可。

set -e

echo "Fun Harness: starting backend"

PROJECT_DIR="/Users/fun/funfundaily/worktrees/invition/backend"
LAUNCH_MODULE="schedule-start"
PORT="8080"
PROFILE="dev"

cd "$PROJECT_DIR"

# ---------- 1) 端口清理 ----------
if command -v lsof >/dev/null 2>&1; then
  PORT_PIDS="$(lsof -t -i:"$PORT" 2>/dev/null || true)"
  if [ -n "$PORT_PIDS" ]; then
    echo "Fun Harness: killing PIDs on :$PORT -> $PORT_PIDS"
    # shellcheck disable=SC2086
    kill $PORT_PIDS 2>/dev/null || true
    sleep 1
    STILL="$(lsof -t -i:"$PORT" 2>/dev/null || true)"
    if [ -n "$STILL" ]; then
      echo "Fun Harness: force killing $STILL"
      # shellcheck disable=SC2086
      kill -9 $STILL 2>/dev/null || true
    fi
  fi
fi

# ---------- 2) 进程兜底清理（仅本目录下的残留 Java 进程） ----------
if command -v pgrep >/dev/null 2>&1; then
  STRAY="$(pgrep -f "${PROJECT_DIR}.*(spring-boot:run|java -jar|ScheduleApplication)" 2>/dev/null || true)"
  if [ -n "$STRAY" ]; then
    echo "Fun Harness: killing stray project PIDs -> $STRAY"
    # shellcheck disable=SC2086
    kill -9 $STRAY 2>/dev/null || true
  fi
fi

# ---------- 3) 把依赖模块（schedule-service / schedule-web）装进本地 m2 ----------
# 必须每次跑一次：schedule-start 通过 jar 依赖引用兄弟模块，源码改动只有 install 后才生效。
# 这里是增量 install（不带 clean），Maven 会跳过未变化的模块，速度很快。
# 不在 schedule-start 上用 `-am ... spring-boot:run`，因为 spring-boot:run 是插件目标，
# 在聚合 pom 上下文里解析不到前缀（会报 "No plugin found for prefix 'spring-boot'"）。
echo "Fun Harness: mvn install (siblings -> local m2, incremental, skip tests)"
mvn -f "$PROJECT_DIR/pom.xml" -DskipTests install

# ---------- 4) 前台启动 schedule-start (profile=dev) ----------
echo "Fun Harness: spring-boot:run on $LAUNCH_MODULE (profile=$PROFILE)"
cd "$PROJECT_DIR/$LAUNCH_MODULE"
exec mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
