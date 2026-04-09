#!/bin/bash

JAR_NAME="forgather-0.0.1-SNAPSHOT.jar"
PORT=8080
PROFILE="prod"
APP_DIR="/home/ubuntu/forgather-backend"
LOG_DIR="$APP_DIR/logs"
JAR_PATH="$APP_DIR/$JAR_NAME"
NOHUP_LOG="$LOG_DIR/nohup.log"

echo "기존 $PORT 포트 프로세스 종료 시도..."
PID=$(sudo lsof -t -i:$PORT)

if [ -n "$PID" ]; then
  echo "PID $PID 종료 중..."
  kill -9 $PID
  echo "프로세스 종료 완료"
else
  echo "$PORT 포트에 실행 중인 프로세스 없음"
fi

echo "디렉토리 및 파일 소유자 변경..."
sudo chown -R ubuntu:ubuntu "$APP_DIR"
sudo chmod 744 "$JAR_PATH"

echo "로그 디렉토리 생성..."
sudo -u ubuntu mkdir -p "$LOG_DIR"

echo "ubuntu 사용자로 애플리케이션 실행 (포트: $PORT, 프로파일: $PROFILE)..."
sudo -u ubuntu nohup java -jar "$JAR_PATH" \
  --server.port=$PORT \
  --spring.profiles.active=$PROFILE \
  >> "$NOHUP_LOG" 2>&1 &

echo "10초 후 프로세스 실행 확인..."
sleep 10

if pgrep -f "$JAR_NAME" > /dev/null; then
  echo "애플리케이션 프로세스 실행 확인"
  echo "stdout 로그: $NOHUP_LOG"
  echo "애플리케이션 로그: $LOG_DIR/app.log (logback)"
else
  echo "애플리케이션 프로세스 실행 실패"
  echo "stdout 로그 마지막 50줄:"
  sudo tail -50 "$NOHUP_LOG" 2>/dev/null || echo "(로그 파일 없음)"
  exit 1
fi

echo "배포 완료"
