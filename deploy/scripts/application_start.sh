#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/common.sh"

# 배포 대상 슬롯 상태 복원
restore_state

# 배포 대상 슬롯 재시작
log "ApplicationStart" "starting service=$NEXT_SERVICE port=$NEXT_PORT"

systemctl stop "$NEXT_SERVICE" 2>/dev/null || true
systemctl start "$NEXT_SERVICE"

# 배포 대상 슬롯 헬스체크
log "ApplicationStart" "헬스체크 시작 (최대 ${HEALTH_TIMEOUT}초)"
ELAPSED=0

while true; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 \
    "http://localhost:${NEXT_PORT}/actuator/health" || echo "000")

  if [[ "$HTTP_CODE" == "200" ]]; then
    log "ApplicationStart" "헬스체크 성공 (${ELAPSED}초 경과)"
    break
  fi

  if [[ "$ELAPSED" -ge "$HEALTH_TIMEOUT" ]]; then
    log "ApplicationStart" "헬스체크 실패 - recent journal"
    journalctl -u "$NEXT_SERVICE" --no-pager -n 100 || true
    exit 1
  fi

  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

log "ApplicationStart" "완료"
