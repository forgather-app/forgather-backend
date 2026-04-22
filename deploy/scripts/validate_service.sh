#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/common.sh"

# 배포 대상 슬롯 상태 복원
restore_state

# nginx 업스트림 변경
log "ValidateService" "nginx upstream -> $NEXT"
ln -sfn "$NEXT_UPSTREAM" "$NGINX_UPSTREAM_ACTIVE"

# 실패 시 롤백
if ! nginx -t; then
  log "ValidateService" "nginx 설정 오류 - upstream 롤백"
  ln -sfn "$PREV_UPSTREAM" "$NGINX_UPSTREAM_ACTIVE"
  exit 1
fi

# nginx 재가동
nginx -s reload
log "ValidateService" "nginx reload 완료"

# nginx를 통한 트래픽 헬스체크
NGINX_HEALTH_TIMEOUT=30
ELAPSED=0
log "ValidateService" "nginx 트래픽 헬스체크 시작 (최대 ${NGINX_HEALTH_TIMEOUT}초)"

while true; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 -L -k \
    "http://localhost/actuator/health" || echo "000")

  if [[ "$HTTP_CODE" == "200" ]]; then
    log "ValidateService" "nginx 트래픽 헬스체크 성공 (${ELAPSED}초 경과)"
    break
  fi

  if [[ "$ELAPSED" -ge "$NGINX_HEALTH_TIMEOUT" ]]; then
    log "ValidateService" "nginx 트래픽 헬스체크 실패 - upstream 롤백"
    ln -sfn "$PREV_UPSTREAM" "$NGINX_UPSTREAM_ACTIVE"
    nginx -s reload
    exit 1
  fi

  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

echo "$NEXT" > "$ACTIVE_FILE"
chmod 644 "$ACTIVE_FILE"

rm -f "$STATE_FILE"

log "ValidateService" "배포 완료: active=$NEXT"
