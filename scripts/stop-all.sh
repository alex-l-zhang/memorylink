#!/usr/bin/env bash
# 停止 start-all.sh 启动的进程（保留 PG，不删容器数据）
cd "$(dirname "$0")/.."

for name in backend web app-web; do
  pid_file="logs/$name.pid"
  if [ -f "$pid_file" ]; then
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      kill -TERM -"$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
      echo "已停止 $name ($pid)"
    else
      echo "$name 未在运行"
    fi
    rm -f "$pid_file"
  fi
done

echo "停止 Redis/MinIO 容器（数据保留）..."
docker-compose -f docker-compose.dev.yml stop 2>/dev/null || true
echo "完成"
