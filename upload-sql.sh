#!/usr/bin/env bash
# upload-sql.sh — 把 script/update_sql.sql（或指定的 SQL 文件）scp 到生产服务器
#
# 用法：
#   ./upload-sql.sh                       # 上传 backend/script/update_sql.sql
#   ./upload-sql.sh path/to/other.sql     # 上传指定文件
#
# 可选环境变量覆盖：
#   REMOTE_HOST   默认 root@120.24.30.25
#   REMOTE_DIR    默认 /root/stack/temp/
#   SSH_KEY       默认 ~/Documents/aliyun/funMacStudio.pem

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REMOTE_HOST="${REMOTE_HOST:-root@120.24.30.25}"
REMOTE_DIR="${REMOTE_DIR:-/root/stack/temp/}"
SSH_KEY="${SSH_KEY:-$HOME/Documents/aliyun/funMacStudio.pem}"

SQL_FILE="${1:-script/update_sql.sql}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "    错误：找不到 SQL 文件：$SQL_FILE"
  exit 1
fi

if [[ ! -f "$SSH_KEY" ]]; then
  echo "    错误：SSH key 不存在：$SSH_KEY"
  exit 1
fi

echo "==> 上传 $SQL_FILE ($(du -h "$SQL_FILE" | awk '{print $1}')) -> ${REMOTE_HOST}:${REMOTE_DIR}"
scp -i "$SSH_KEY" "$SQL_FILE" "${REMOTE_HOST}:${REMOTE_DIR}"

echo "==> 完成。远端路径：${REMOTE_HOST}:${REMOTE_DIR}$(basename "$SQL_FILE")"
