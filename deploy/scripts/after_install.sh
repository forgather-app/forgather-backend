#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/common.sh"

mkdir -p "$DEPLOY_DIR/scripts"
chmod 755 "$DEPLOY_DIR/scripts"/*.sh

# 배포 대상 슬롯 상태 설정
load_slot_context
save_state

# jar 파일 배치
log "AfterInstall" "PROFILE=$PROFILE | CURRENT=$CURRENT -> NEXT=$NEXT (port $NEXT_PORT)"
log "AfterInstall" "jar 복사: $APP_DIR/$JAR_NAME -> $NEXT_DIR/app.jar"

mkdir -p "$NEXT_DIR/logs"
cp "$APP_DIR/$JAR_NAME" "$NEXT_DIR/app.jar"
chown -R ubuntu:ubuntu "$NEXT_DIR"
chmod -R 755 "$NEXT_DIR"
chmod 644 "$NEXT_DIR/app.jar"

log "AfterInstall" "완료"
