#!/bin/bash
set -euo pipefail

# ─── 상수 ────────────────────────────────────────────────────────────────────
JAR_NAME="forgather-0.0.1-SNAPSHOT.jar"
PROFILE="prod"
APP_DIR="/home/ubuntu/forgather-backend"
BLUE_DIR="/home/ubuntu/forgather-blue"
GREEN_DIR="/home/ubuntu/forgather-green"
BLUE_PORT=8081
GREEN_PORT=8082
ACTIVE_FILE="/etc/forgather/active"
NGINX_UPSTREAM_ACTIVE="/etc/nginx/upstream-active.conf"
HEALTH_TIMEOUT=90

# ─── 현재 활성 슬롯 파악 ──────────────────────────────────────────────────────
CURRENT=$(cat "$ACTIVE_FILE" 2>/dev/null || echo "blue")
if [[ "$CURRENT" == "blue" ]]; then
  NEXT="green"
  NEXT_DIR="$GREEN_DIR"
  NEXT_PORT="$GREEN_PORT"
  NEXT_SERVICE="forgather-green"
  NEXT_UPSTREAM="/etc/nginx/upstream-green.conf"
else
  NEXT="blue"
  NEXT_DIR="$BLUE_DIR"
  NEXT_PORT="$BLUE_PORT"
  NEXT_SERVICE="forgather-blue"
  NEXT_UPSTREAM="/etc/nginx/upstream-blue.conf"
fi

echo "[deploy] PROFILE=$PROFILE | CURRENT=$CURRENT → NEXT=$NEXT (port $NEXT_PORT)"

# ─── jar 복사 ─────────────────────────────────────────────────────────────────
echo "[deploy] jar 복사: $APP_DIR/$JAR_NAME → $NEXT_DIR/app.jar"
mkdir -p "$NEXT_DIR/logs"
cp "$APP_DIR/$JAR_NAME" "$NEXT_DIR/app.jar"
chown -R ubuntu:ubuntu "$NEXT_DIR"

# ─── 비활성 슬롯 재시작 ───────────────────────────────────────────────────────
echo "[deploy] $NEXT_SERVICE 재시작..."
systemctl stop "$NEXT_SERVICE" 2>/dev/null || true
systemctl start "$NEXT_SERVICE"

# ─── 헬스체크 ─────────────────────────────────────────────────────────────────
echo "[deploy] 헬스체크 시작 (최대 ${HEALTH_TIMEOUT}초)..."
ELAPSED=0
while true; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://localhost:${NEXT_PORT}/actuator/health" || echo "000")

  if [[ "$HTTP_CODE" == "200" ]]; then
    echo "[deploy] 헬스체크 성공 (${ELAPSED}초 경과)"
    break
  fi

  if [[ "$ELAPSED" -ge "$HEALTH_TIMEOUT" ]]; then
    echo "[deploy] 헬스체크 실패 — 최근 로그:"
    journalctl -u "$NEXT_SERVICE" --no-pager -n 100 || true
    exit 1
  fi

  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

# ─── nginx upstream 전환 ──────────────────────────────────────────────────────
echo "[deploy] nginx upstream → $NEXT"
ln -sfn "$NEXT_UPSTREAM" "$NGINX_UPSTREAM_ACTIVE"

if ! nginx -t 2>/dev/null; then
  echo "[deploy] nginx 설정 오류 — upstream 롤백"
  PREV_UPSTREAM="/etc/nginx/upstream-${CURRENT}.conf"
  ln -sfn "$PREV_UPSTREAM" "$NGINX_UPSTREAM_ACTIVE"
  exit 1
fi

nginx -s reload
echo "[deploy] nginx reload 완료"

# ─── active 파일 업데이트 ─────────────────────────────────────────────────────
echo "$NEXT" > "$ACTIVE_FILE"
echo "[deploy] 배포 완료: active=$NEXT"
