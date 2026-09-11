#!/usr/bin/env bash
# ==============================================================================
# CareBridge - Run All Services (All-or-Nothing Strict Mode)
# Quy tắc: TẤT CẢ 4 services phải khởi động thành công.
# Nếu BẤT KỲ service nào lỗi hoặc dừng đột ngột -> HỦY VÀ KILL SẠCH TẤT CẢ.
# ==============================================================================

set -eo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"
mkdir -p "$LOGS_DIR"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

abort_all() {
  local reason="$1"
  local failed_log="$2"

  echo -e "\n${RED}======================================================${NC}"
  echo -e "${RED}${BOLD}  ❌ KHỞI ĐỘNG THẤT BẠI: ${reason}${NC}"
  echo -e "${RED}  Đang tiến hành hủy và kill toàn bộ các service khác...${NC}"
  echo -e "${RED}======================================================${NC}"

  # Hiển thị log lỗi gần nhất nếu có
  if [ -n "$failed_log" ] && [ -f "$failed_log" ]; then
    echo -e "\n${YELLOW}--- Chi tiết lỗi từ file log (${failed_log}): ---${NC}"
    tail -n 25 "$failed_log"
    echo -e "${YELLOW}--------------------------------------------------${NC}\n"
  fi

  # Dừng sạch sẽ tất cả
  "$PROJECT_ROOT/stop-all.sh" all >/dev/null 2>&1 || true

  echo -e "${RED}✗ Đã hủy toàn bộ và giải phóng sạch sẽ tất cả các port.${NC}\n"
  exit 1
}

echo -e "${BLUE}======================================================${NC}"
echo -e "${BOLD}   🚀 CareBridge - Starting All Services (Strict Mode) ${NC}"
echo -e "${BLUE}======================================================${NC}"

# ------------------------------------------------------------------------------
# 1. PRE-FLIGHT CHECKS (Kiểm tra điều kiện tiên quyết)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[1/6] Kiểm tra điều kiện tiên quyết trước khi chạy...${NC}"

# Check Docker
if ! command -v docker >/dev/null 2>&1; then
  abort_all "Lệnh 'docker' chưa được cài đặt trên máy!" ""
fi

if ! docker info >/dev/null 2>&1; then
  abort_all "Docker Desktop chưa được bật! Cần bật Docker để chạy exercise_correction_sidecar." ""
fi

# Check thư mục dự án
API_DIR="$PROJECT_ROOT/05_Development/CareBridgeAPI"
AI_DIR="$PROJECT_ROOT/05_Development/CareBridgeAITriageService"
SIDECAR_DIR="$PROJECT_ROOT/05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar"
WEB_DIR="$PROJECT_ROOT/05_Development/CareBridgeWebApp"

for d in "$API_DIR" "$AI_DIR" "$SIDECAR_DIR" "$WEB_DIR"; do
  if [ ! -d "$d" ]; then
    abort_all "Không tìm thấy thư mục: $d" ""
  fi
done

echo -e "${GREEN}✓ Tất cả điều kiện tiên quyết hợp lệ (Docker, thư mục dự án).${NC}"

# ------------------------------------------------------------------------------
# 2. DỌN DẸP PORT CŨ
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[2/6] Dọn dẹp các tiến trình cũ và giải phóng port...${NC}"
"$PROJECT_ROOT/stop-all.sh" all >/dev/null 2>&1 || true

# ------------------------------------------------------------------------------
# 3. BUILD & RUN DOCKER SIDECAR (Port 8002)
# ------------------------------------------------------------------------------
echo -e "\n${BLUE}[3/6] Build & chạy container exercise_correction_sidecar (Port 8002)...${NC}"
if ! (cd "$SIDECAR_DIR" && docker build -t exercise-correction . > "$LOGS_DIR/exercise_correction_build.log" 2>&1); then
  abort_all "Docker build exercise-correction thất bại!" "$LOGS_DIR/exercise_correction_build.log"
fi

docker rm -f exercise-correction >/dev/null 2>&1 || true
if ! docker run -d --rm --name exercise-correction -p 8002:8002 exercise-correction > "$LOGS_DIR/exercise_correction.log" 2>&1; then
  abort_all "Docker run exercise-correction thất bại!" "$LOGS_DIR/exercise_correction.log"
fi
echo -e "${GREEN}✓ Docker container 'exercise-correction' đã khởi tạo.${NC}"

# ------------------------------------------------------------------------------
# 4. KHỞI ĐỘNG CÁC SERVICES CÒN LẠI
# ------------------------------------------------------------------------------

# 4.1 CareBridgeAITriageService (FastAPI - Port 8001)
echo -e "\n${BLUE}[4/6] Khởi động CareBridgeAITriageService (FastAPI: 8001)...${NC}"
(
  cd "$AI_DIR"
  [ -f .env ] && set -a && source .env && set +a
  if [ -f "./venv/bin/uvicorn" ]; then
    exec ./venv/bin/uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
  else
    exec uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
  fi
) > "$LOGS_DIR/carebridge_ai_triage.log" 2>&1 &
AI_PID=$!
echo "$AI_PID" > "$LOGS_DIR/carebridge_ai_triage.pid"

# 4.2 CareBridgeWebApp (React/Vite - Port 5173)
echo -e "${BLUE}[5/6] Khởi động CareBridgeWebApp (React/Vite: 5173)...${NC}"
(
  cd "$WEB_DIR"
  exec npm run dev
) > "$LOGS_DIR/carebridge_webapp.log" 2>&1 &
WEB_PID=$!
echo "$WEB_PID" > "$LOGS_DIR/carebridge_webapp.pid"

# 4.3 CareBridgeAPI (Spring Boot - Port 8080)
echo -e "${BLUE}[6/6] Khởi động CareBridgeAPI (Spring Boot: 8080)...${NC}"
(
  cd "$API_DIR"
  [ -f .env ] && set -a && source .env && set +a
  exec ./mvnw spring-boot:run
) > "$LOGS_DIR/carebridge_api.log" 2>&1 &
API_PID=$!
echo "$API_PID" > "$LOGS_DIR/carebridge_api.pid"

# ------------------------------------------------------------------------------
# 5. GIÁM SÁT TRẠNG THÁI KHỞI ĐỘNG (ALL-OR-NOTHING GATE)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}⏳ Đang giám sát trạng thái mở port của cả 4 services...${NC}"

API_READY=0
AI_READY=0
SIDECAR_READY=0
WEB_READY=0

MAX_TIMEOUT=60
ELAPSED=0

while [ $ELAPSED -lt $MAX_TIMEOUT ]; do
  # Kiểm tra Docker Sidecar (Port 8002)
  if [ $SIDECAR_READY -eq 0 ]; then
    if lsof -ti :8002 -sTCP:LISTEN >/dev/null 2>&1; then
      SIDECAR_READY=1
      echo -e "  ${GREEN}✓ exercise_correction_sidecar đã sẵn sàng trên port 8002!${NC}"
    elif ! docker ps -q -f name=exercise-correction | grep -q .; then
      abort_all "Docker container 'exercise-correction' dừng đột ngột!" "$LOGS_DIR/exercise_correction.log"
    fi
  fi

  # Kiểm tra AI Triage (Port 8001)
  if [ $AI_READY -eq 0 ]; then
    if lsof -ti :8001 -sTCP:LISTEN >/dev/null 2>&1; then
      AI_READY=1
      echo -e "  ${GREEN}✓ CareBridgeAITriageService đã sẵn sàng trên port 8001!${NC}"
    elif ! ps -p "$AI_PID" >/dev/null 2>&1; then
      abort_all "CareBridgeAITriageService dừng đột ngột trước khi mở port 8001!" "$LOGS_DIR/carebridge_ai_triage.log"
    fi
  fi

  # Kiểm tra WebApp (Port 5173)
  if [ $WEB_READY -eq 0 ]; then
    if lsof -ti :5173 -sTCP:LISTEN >/dev/null 2>&1; then
      WEB_READY=1
      echo -e "  ${GREEN}✓ CareBridgeWebApp đã sẵn sàng trên port 5173!${NC}"
    elif ! ps -p "$WEB_PID" >/dev/null 2>&1; then
      abort_all "CareBridgeWebApp dừng đột ngột trước khi mở port 5173!" "$LOGS_DIR/carebridge_webapp.log"
    fi
  fi

  # Kiểm tra API Spring Boot (Port 8080)
  if [ $API_READY -eq 0 ]; then
    if lsof -ti :8080 -sTCP:LISTEN >/dev/null 2>&1; then
      API_READY=1
      echo -e "  ${GREEN}✓ CareBridgeAPI đã sẵn sàng trên port 8080!${NC}"
    elif ! ps -p "$API_PID" >/dev/null 2>&1; then
      abort_all "CareBridgeAPI dừng đột ngột trước khi mở port 8080!" "$LOGS_DIR/carebridge_api.log"
    fi
  fi

  # Nếu cả 4 đã sẵn sàng -> Thành công!
  if [ $API_READY -eq 1 ] && [ $AI_READY -eq 1 ] && [ $SIDECAR_READY -eq 1 ] && [ $WEB_READY -eq 1 ]; then
    break
  fi

  sleep 2
  ELAPSED=$((ELAPSED + 2))
done

# Kiểm tra nếu hết timeout mà vẫn còn service chưa lên
if [ $API_READY -eq 0 ] || [ $AI_READY -eq 0 ] || [ $SIDECAR_READY -eq 0 ] || [ $WEB_READY -eq 0 ]; then
  if [ $SIDECAR_READY -eq 0 ]; then
    abort_all "exercise_correction_sidecar quá thời gian chờ (timeout ${MAX_TIMEOUT}s)!" "$LOGS_DIR/exercise_correction.log"
  fi
  if [ $AI_READY -eq 0 ]; then
    abort_all "CareBridgeAITriageService quá thời gian chờ (timeout ${MAX_TIMEOUT}s)!" "$LOGS_DIR/carebridge_ai_triage.log"
  fi
  if [ $WEB_READY -eq 0 ]; then
    abort_all "CareBridgeWebApp quá thời gian chờ (timeout ${MAX_TIMEOUT}s)!" "$LOGS_DIR/carebridge_webapp.log"
  fi
  if [ $API_READY -eq 0 ]; then
    abort_all "CareBridgeAPI quá thời gian chờ (timeout ${MAX_TIMEOUT}s)!" "$LOGS_DIR/carebridge_api.log"
  fi
fi

# ------------------------------------------------------------------------------
# 6. DASHBOARD THÀNH CÔNG
# ------------------------------------------------------------------------------
echo -e "\n${GREEN}======================================================${NC}"
echo -e "${BOLD}   🎉 TẤT CẢ 4 SERVICES ĐÃ KHỞI ĐỘNG THÀNH CÔNG!     ${NC}"
echo -e "${GREEN}======================================================${NC}"
echo -e "┌──────────────────────────────┬────────┬──────────────────────────────┐"
echo -e "│ Service                      │ Port   │ URL / Endpoint               │"
echo -e "├──────────────────────────────┼────────┼──────────────────────────────┤"
echo -e "│ CareBridgeWebApp (Frontend)  │ 5173   │ http://localhost:5173        │"
echo -e "│ CareBridgeAPI (Backend)      │ 8080   │ http://localhost:8080        │"
echo -e "│ AI Triage Service (FastAPI)  │ 8001   │ http://localhost:8001/docs   │"
echo -e "│ Exercise Sidecar (Docker)    │ 8002   │ http://localhost:8002        │"
echo -e "└──────────────────────────────┴────────┴──────────────────────────────┘"

echo -e "\n${BOLD}📋 Lệnh nhanh tiện ích:${NC}"
echo -e "  - Kiểm tra trạng thái:   ${YELLOW}./run status${NC}"
echo -e "  - Xem log Backend:       ${YELLOW}tail -f logs/carebridge_api.log${NC}"
echo -e "  - Xem log Sidecar:       ${YELLOW}tail -f logs/exercise_correction.log${NC}"
echo -e "  - Xem tất cả log:        ${YELLOW}tail -f logs/*.log${NC}"
echo -e "  - Dừng tất cả dịch vụ:   ${RED}./stop all${NC}"
echo ""
