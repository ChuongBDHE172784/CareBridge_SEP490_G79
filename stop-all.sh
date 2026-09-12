#!/usr/bin/env bash
# ==============================================================================
# CareBridge - Stop All Services
# Usage: ./stop-all.sh [all|api|ai|sidecar|web]
# ==============================================================================

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

# Terminal Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TARGET="${1:-all}"

kill_by_port() {
  local port=$1
  local name=$2
  local pids
  pids=$(lsof -ti :"$port" 2>/dev/null)
  if [ -n "$pids" ]; then
    echo -e "${YELLOW}Stopping $name on port $port (PID: $pids)...${NC}"
    for pid in $pids; do
      kill -TERM "$pid" 2>/dev/null || true
    done
    sleep 1
    local remaining
    remaining=$(lsof -ti :"$port" 2>/dev/null)
    if [ -n "$remaining" ]; then
      for pid in $remaining; do
        kill -9 "$pid" 2>/dev/null || true
      done
    fi
    echo -e "${GREEN}✓ $name stopped.${NC}"
  else
    echo -e "${BLUE}ℹ Port $port ($name) is already free.${NC}"
  fi
}

kill_by_pidfile() {
  local pidfile=$1
  local name=$2
  if [ -f "$pidfile" ]; then
    local pid
    pid=$(cat "$pidfile" 2>/dev/null)
    if [ -n "$pid" ] && ps -p "$pid" >/dev/null 2>&1; then
      echo -e "${YELLOW}Stopping $name from PID file (PID: $pid)...${NC}"
      kill -TERM "$pid" 2>/dev/null || true
      sleep 1
      if ps -p "$pid" >/dev/null 2>&1; then
        kill -9 "$pid" 2>/dev/null || true
      fi
    fi
    rm -f "$pidfile"
  fi
}

stop_api() {
  echo -e "\n${BLUE}--- Stopping CareBridgeAPI (port 8080) ---${NC}"
  kill_by_pidfile "$LOGS_DIR/carebridge_api.pid" "CareBridgeAPI"
  kill_by_port 8080 "CareBridgeAPI"
  pkill -f "CareBridgeAPI.*spring-boot" 2>/dev/null || true
}

stop_ai() {
  echo -e "\n${BLUE}--- Stopping CareBridgeAITriageService (port 8001) ---${NC}"
  kill_by_pidfile "$LOGS_DIR/carebridge_ai_triage.pid" "CareBridgeAITriage"
  kill_by_port 8001 "CareBridgeAITriage"
  pkill -f "uvicorn.*app.main:app.*8001" 2>/dev/null || true
}

stop_sidecar() {
  echo -e "\n${BLUE}--- Stopping exercise_correction_sidecar (port 8002) ---${NC}"
  kill_by_pidfile "$LOGS_DIR/exercise_correction.pid" "ExerciseCorrectionSidecar"
  kill_by_port 8002 "ExerciseCorrectionSidecar"
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    if docker ps -a --format '{{.Names}}' | grep -Eq "^exercise-correction$"; then
      echo -e "${YELLOW}Removing Docker container 'exercise-correction'...${NC}"
      docker rm -f exercise-correction >/dev/null 2>&1 || true
      echo -e "${GREEN}✓ Docker container 'exercise-correction' removed.${NC}"
    fi
  fi
}

stop_web() {
  echo -e "\n${BLUE}--- Stopping CareBridgeWebApp (port 5173) ---${NC}"
  kill_by_pidfile "$LOGS_DIR/carebridge_webapp.pid" "CareBridgeWebApp"
  kill_by_port 5173 "CareBridgeWebApp"
  kill_by_port 3000 "CareBridgeWebApp (alt)"
}

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}      CareBridge Services Shutdown       ${NC}"
echo -e "${YELLOW}=========================================${NC}"

case "$TARGET" in
  api)
    stop_api
    ;;
  ai)
    stop_ai
    ;;
  sidecar|docker)
    stop_sidecar
    ;;
  web|webapp)
    stop_web
    ;;
  all|"")
    stop_api
    stop_ai
    stop_sidecar
    stop_web
    echo -e "\n${GREEN}=========================================${NC}"
    echo -e "${GREEN}✓ All CareBridge services stopped cleanly!${NC}"
    echo -e "${GREEN}=========================================${NC}\n"
    ;;
  *)
    echo -e "${RED}Unknown target: $TARGET${NC}"
    echo "Usage: ./stop-all.sh [all|api|ai|sidecar|web]"
    exit 1
    ;;
esac
