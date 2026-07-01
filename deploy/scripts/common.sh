#!/bin/bash
set -euo pipefail

JAR_NAME="forgather-0.0.1-SNAPSHOT.jar"
PROFILE="prod"

APP_DIR="/home/ubuntu/forgather-backend"
DEPLOY_DIR="/home/ubuntu/forgather-backend"

BLUE_DIR="/home/ubuntu/forgather-blue"
GREEN_DIR="/home/ubuntu/forgather-green"

BLUE_PORT=8081
GREEN_PORT=8082

BLUE_SERVICE="forgather-blue"
GREEN_SERVICE="forgather-green"

ACTIVE_FILE="/etc/forgather/active"
STATE_FILE="/var/tmp/forgather-deploy-state.env"

NGINX_UPSTREAM_ACTIVE="/etc/nginx/upstream-active.conf"
BLUE_UPSTREAM="/etc/nginx/upstream-blue.conf"
GREEN_UPSTREAM="/etc/nginx/upstream-green.conf"

HEALTH_TIMEOUT=90

log() {
  echo "[deploy][$1] ${2:-}"
}

# 배포 대상 슬롯 상태 설정
load_slot_context() {
  mkdir -p "$(dirname "$ACTIVE_FILE")"
  CURRENT=$(cat "$ACTIVE_FILE" 2>/dev/null || echo "blue")

  if [[ "$CURRENT" == "blue" ]]; then
    NEXT="green"
    NEXT_DIR="$GREEN_DIR"
    NEXT_PORT="$GREEN_PORT"
    NEXT_SERVICE="$GREEN_SERVICE"
    NEXT_UPSTREAM="$GREEN_UPSTREAM"
    PREV_SERVICE="$BLUE_SERVICE"
    PREV_UPSTREAM="$BLUE_UPSTREAM"
  else
    NEXT="blue"
    NEXT_DIR="$BLUE_DIR"
    NEXT_PORT="$BLUE_PORT"
    NEXT_SERVICE="$BLUE_SERVICE"
    NEXT_UPSTREAM="$BLUE_UPSTREAM"
    PREV_SERVICE="$GREEN_SERVICE"
    PREV_UPSTREAM="$GREEN_UPSTREAM"
  fi
}

# 여러 배포 단계에서 사용할 슬롯 컨텍스트 변수 보관
save_state() {
  cat > "$STATE_FILE" <<EOF
CURRENT=$CURRENT
NEXT=$NEXT
NEXT_DIR=$NEXT_DIR
NEXT_PORT=$NEXT_PORT
NEXT_SERVICE=$NEXT_SERVICE
NEXT_UPSTREAM=$NEXT_UPSTREAM
PREV_SERVICE=$PREV_SERVICE
PREV_UPSTREAM=$PREV_UPSTREAM
EOF
  chmod 600 "$STATE_FILE"
}

# state_file 있으면 불러오고, 없으면 초기화
restore_state() {
  if [[ -f "$STATE_FILE" ]]; then
    # shellcheck disable=SC1090
    source "$STATE_FILE"
  else
    load_slot_context
    save_state
  fi
}
