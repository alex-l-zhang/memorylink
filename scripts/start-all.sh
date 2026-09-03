#!/usr/bin/env bash
# 忆联本地开发一键启动：Redis/MinIO + 后端 + B 端 web + 家属端(Flutter Web)
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p logs

echo "[1/4] 启动基础设施（Redis + MinIO）..."
docker-compose -f docker-compose.dev.yml up -d
for _ in $(seq 1 30); do
  if curl -sf http://localhost:9000/minio/health/live >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! pg_isready -h localhost -p 5432 -q 2>/dev/null; then
  echo "警告：PostgreSQL 未在 5432 监听，请先启动本机 PG 服务"
fi

start() {
  local name="$1"
  shift
  setsid "$@" >"logs/$name.log" 2>&1 &
  echo $! >"logs/$name.pid"
  echo "  已启动 $name (pid $!)"
}

echo "[2/4] 启动后端 (8080)..."
start backend bash -c 'cd server && set -a && source ../.env && set +a && exec mvn -q -B spring-boot:run'

echo "[3/4] 启动 B 端 web (5173)..."
start web bash -c 'cd web && exec npm run dev'

echo "[4/4] 启动家属端 Flutter Web (5180)..."
start app-web bash -c 'export PATH=/home/dev/flutter/bin:$PATH; cd app && exec flutter run -d web-server --web-hostname 0.0.0.0 --web-port 5180 --dart-define=API_BASE=http://192.168.32.128:8080'

sleep 3
cat <<'EOF'

已启动，访问地址：
  家属端（C 端 Web） : http://192.168.32.128:5180
  B 端机构后台       : http://192.168.32.128:5173
  平台后台管理       : http://192.168.32.128:5174（未启动，需要时：npm --prefix admin run dev）
  后端接口文档       : http://192.168.32.128:8080/swagger-ui.html

查看日志：
  tail -f logs/backend.log   # 后端
  tail -f logs/app-web.log   # 家属端
  tail -f logs/web.log       # B 端 web

停止全部：scripts/stop-all.sh
EOF
