#!/usr/bin/env bash
# 忆联本地开发一键启动：Redis/MinIO + 后端 + B 端 web + 家属端(Flutter Web)
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p logs

echo "[0/5] 端口预检..."
for port in 8080 5173 5180 5190; do
  if ss -ltn 2>/dev/null | grep -q ":$port "; then
    echo "错误：端口 $port 已被占用。请先执行 scripts/stop-all.sh 清理残留进程。"
    exit 1
  fi
done

echo "[1/5] 启动基础设施（Redis + MinIO）..."
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

echo "[2/5] 启动后端 (8080)..."
start backend bash -c 'cd server && set -a && source ../.env && set +a && exec mvn -q -B spring-boot:run'

echo "[3/5] 启动 B 端 web (5173)..."
start web bash -c 'cd web && exec npm run dev'

echo "[4/5] 启动家属端 Flutter Web (5180)..."
start app-web bash -c 'export PATH=/home/dev/flutter/bin:$PATH; cd app && exec flutter run -d web-server --web-hostname 0.0.0.0 --web-port 5180 --dart-define=API_BASE=http://192.168.32.128:8080'

echo "[5/5] 启动设计原型 (5190)..."
start prototype bash -c 'cd prototype && exec npm run dev'

sleep 3

wait_http() {
  local url="$1"
  local timeout="$2"
  for _ in $(seq 1 "$timeout"); do
    if curl -sf -o /dev/null "$url" 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  return 1
}

echo "等待服务就绪..."
backend_ok=no
web_ok=no
app_ok=no
proto_ok=no
wait_http http://localhost:8080/api/v1/ping 60 && backend_ok=yes
wait_http http://localhost:5173/ 60 && web_ok=yes
# 家属端首次编译较慢，最长等待 3 分钟
wait_http http://localhost:5180/ 180 && app_ok=yes
wait_http http://localhost:5190/ 60 && proto_ok=yes

echo
echo "启动结果："
[ "$backend_ok" = yes ] && echo "  ✅ 后端 (8080)" || echo "  ❌ 后端 (8080) 启动失败，查看 logs/backend.log"
[ "$web_ok" = yes ] && echo "  ✅ B 端 web (5173)" || echo "  ❌ B 端 web (5173) 启动失败，查看 logs/web.log"
[ "$app_ok" = yes ] && echo "  ✅ 家属端 (5180)" || echo "  ❌ 家属端 (5180) 启动失败，查看 logs/app-web.log"
[ "$proto_ok" = yes ] && echo "  ✅ 设计原型 (5190)" || echo "  ❌ 设计原型 (5190) 启动失败，查看 logs/prototype.log"

cat <<'EOF'

已启动，访问地址：
  家属端（C 端 Web） : http://192.168.32.128:5180
  B 端机构后台       : http://192.168.32.128:5173
  设计原型（身后空间）: http://192.168.32.128:5190
  平台后台管理       : http://192.168.32.128:5174（未启动，需要时：npm --prefix admin run dev）
  后端接口文档       : http://192.168.32.128:8080/swagger-ui.html

查看日志：
  tail -f logs/backend.log   # 后端
  tail -f logs/app-web.log   # 家属端
  tail -f logs/web.log       # B 端 web
  tail -f logs/prototype.log # 设计原型

停止全部：scripts/stop-all.sh
EOF

if [ "$backend_ok" != yes ] || [ "$web_ok" != yes ] || [ "$app_ok" != yes ] || [ "$proto_ok" != yes ]; then
  exit 1
fi
