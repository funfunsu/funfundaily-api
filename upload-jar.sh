#!/usr/bin/env bash
# upload-jar.sh — 打包后端项目并把可执行 jar scp 到生产服务器
#
# 默认流程：
#   1) mvn clean package（跳过测试）构建全部子模块，
#      schedule-start 经 spring-boot-maven-plugin repackage 成可执行 jar
#   2) 把 schedule-start/target/schedule-start-*.jar scp 到远端
#
# 用法：
#   ./upload-jar.sh                 # 构建 + 上传
#   ./upload-jar.sh --skip-build    # 跳过构建，直接上传已有 jar
#
# 可选环境变量覆盖：
#   REMOTE_HOST   默认 root@120.24.30.25
#   REMOTE_DIR    默认 /root/stack/app
#   SSH_KEY       默认 ~/Documents/aliyun/funMacStudio.pem

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REMOTE_HOST="${REMOTE_HOST:-root@120.24.30.25}"
REMOTE_DIR="${REMOTE_DIR:-/root/stack/app}"
SSH_KEY="${SSH_KEY:-$HOME/Documents/aliyun/funMacStudio.pem}"

SKIP_BUILD=""
if [[ "${1:-}" == "--skip-build" ]]; then
  SKIP_BUILD="1"
fi

if [[ ! -f "$SSH_KEY" ]]; then
  echo "    错误：SSH key 不存在：$SSH_KEY"
  exit 1
fi

if [[ -n "$SKIP_BUILD" ]]; then
  echo "==> [1/3] 跳过构建（--skip-build），使用已有 jar"
else
  echo "==> [1/3] mvn clean package -Dmaven.test.skip=true（构建全部子模块）"
  mvn -f pom.xml clean package -Dmaven.test.skip=true
fi

echo "==> [2/3] 定位可执行 jar"
# spring-boot repackage 后：schedule-start-*.jar 为可执行包，*.jar.original 为原始包（已被 glob 排除）
shopt -s nullglob
JARS=( schedule-start/target/schedule-start-*.jar )
shopt -u nullglob

if [[ ${#JARS[@]} -eq 0 ]]; then
  echo "    错误：未找到 jar：schedule-start/target/schedule-start-*.jar"
  echo "    请先去掉 --skip-build 进行构建。"
  exit 1
fi
if [[ ${#JARS[@]} -gt 1 ]]; then
  echo "    错误：匹配到多个 jar，请清理 target 后重试：${JARS[*]}"
  exit 1
fi

JAR_FILE="${JARS[0]}"
echo "    使用：$JAR_FILE ($(du -h "$JAR_FILE" | awk '{print $1}'))"

echo "==> [3/3] 上传 -> ${REMOTE_HOST}:${REMOTE_DIR}/"
# 确保远端目录存在
ssh -i "$SSH_KEY" "$REMOTE_HOST" "mkdir -p '${REMOTE_DIR}'"
scp -i "$SSH_KEY" "$JAR_FILE" "${REMOTE_HOST}:${REMOTE_DIR}/"

echo "==> 完成。远端路径：${REMOTE_HOST}:${REMOTE_DIR}/$(basename "$JAR_FILE")"
