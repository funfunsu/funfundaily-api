#!/usr/bin/env bash
# dev-run.sh — 改完代码后一键启动 schedule-start (dev profile)
#
# 做三件事：
#   1) 杀掉占用 APP_PORT 的旧进程（上一次 spring-boot:run 没退干净时常见）
#   2) 在父 pom 上跑 install，把 schedule-service / schedule-web 的最新源码
#      打成 jar 装进本地 ~/.m2，避免 schedule-start 引用到旧版本依赖
#   3) 启动 schedule-start (spring-boot:run, profile=dev)
#
# 全程跳过测试编译与执行（-Dmaven.test.skip=true），所以测试代码里的
# 编译错误不会挡住启动。需要跑测试时单独执行 `mvn test`。
#
# 用法：
#   ./dev-run.sh              # 增量 install + 启动
#   ./dev-run.sh --clean      # 先 clean 再 install + 启动（彻底重建）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# application.yml 里 server.port=8080；改了端口同步改这里
APP_PORT="${APP_PORT:-8080}"

CLEAN_GOAL=""
if [[ "${1:-}" == "--clean" ]]; then
  CLEAN_GOAL="clean"
  echo "==> 模式：彻底重建 (clean install)"
else
  echo "==> 模式：增量构建 (install)。如需彻底重建请加 --clean"
fi

echo "==> [1/3] 检查并清理占用 :${APP_PORT} 的旧进程"
OLD_PIDS="$(lsof -ti tcp:"${APP_PORT}" -sTCP:LISTEN || true)"
if [[ -n "${OLD_PIDS}" ]]; then
  echo "    发现旧进程 PID: ${OLD_PIDS}，先发 SIGTERM"
  # shellcheck disable=SC2086
  kill ${OLD_PIDS} 2>/dev/null || true
  # 最多等 5 秒优雅退出
  for _ in 1 2 3 4 5; do
    sleep 1
    REMAINING="$(lsof -ti tcp:"${APP_PORT}" -sTCP:LISTEN || true)"
    [[ -z "${REMAINING}" ]] && break
  done
  REMAINING="$(lsof -ti tcp:"${APP_PORT}" -sTCP:LISTEN || true)"
  if [[ -n "${REMAINING}" ]]; then
    echo "    仍未退出，发 SIGKILL: ${REMAINING}"
    # shellcheck disable=SC2086
    kill -9 ${REMAINING} 2>/dev/null || true
  fi
  echo "    端口 :${APP_PORT} 已释放"
else
  echo "    端口 :${APP_PORT} 空闲，跳过"
fi

echo "==> [2/3] mvn ${CLEAN_GOAL} install -Dmaven.test.skip=true (刷新所有子模块到本地 m2)"
mvn -f pom.xml ${CLEAN_GOAL} install -Dmaven.test.skip=true

echo "==> [3/3] spring-boot:run (profile=dev)"
mvn -f schedule-start/pom.xml \
    -Dmaven.test.skip=true \
    spring-boot:run \
    -Dspring-boot.run.profiles=dev
