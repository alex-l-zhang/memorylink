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

# 兜底：按端口停止残留进程（pid 文件失效时的保护）
for port in 8080 5173 5174 5180; do
  pid=$(ss -ltnp 2>/dev/null | grep ":$port " | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2)
  if [ -n "$pid" ]; then
    pgid=$(ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ')
    echo "端口 $port 仍被进程 $pid 占用，停止中..."
    if [ -n "$pgid" ]; then
      kill -TERM -"$pgid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null
    else
      kill -TERM "$pid" 2>/dev/null
    fi
  fi
done
sleep 2
echo "完成"
