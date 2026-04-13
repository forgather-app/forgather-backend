#!/bin/bash
set -euo pipefail

# ─── 상수 ────────────────────────────────────────────────────────────────────
JAR_NAME="forgather-0.0.1-SNAPSHOT.jar"
PROFILE="dev"
APP_DIR="/home/ubuntu/forgather-backend"
SERVICE="forgather"
PORT=8080
HEALTH_TIMEOUT=90

# ─── jar 배치 ─────────────────────────────────────────────────────────────────
echo "[deploy] jar 배치: $APP_DIR/$JAR_NAME"
mkdir -p "$APP_DIR/logs"
chown -R ubuntu:ubuntu "$APP_DIR"

# ─── 서비스 재시작 ────────────────────────────────────────────────────────────
echo "[deploy] $SERVICE 재시작..."
systemctl stop "$SERVICE" 2>/dev/null || true
systemctl start "$SERVICE"

# ─── 헬스체크 ─────────────────────────────────────────────────────────────────
echo "[deploy] 헬스체크 시작 (최대 ${HEALTH_TIMEOUT}초)..."
ELAPSED=0
while true; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 \
    "http://localhost:${PORT}/actuator/health" || echo "000")

  if [[ "$HTTP_CODE" == "200" ]]; then
    echo "[deploy] 헬스체크 성공 (${ELAPSED}초 경과)"
    break
  fi

  if [[ "$ELAPSED" -ge "$HEALTH_TIMEOUT" ]]; then
    echo "[deploy] 헬스체크 실패 — 최근 로그:"
    journalctl -u "$SERVICE" --no-pager -n 100 || true
    exit 1
  fi

  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

echo "[deploy] 배포 완료: PROFILE=$PROFILE port=$PORT"
