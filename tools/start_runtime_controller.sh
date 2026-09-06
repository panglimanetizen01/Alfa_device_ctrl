#!/usr/bin/env bash
# Start the local Android controller server for one explicit pipeline run.
set +e
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-run_20260823_171233_30513}"
PORT="${2:-8091}"
PID_FILE="$HOME/.alfa_device_ctrl_controller.pid"
LOG_FILE="$HOME/.alfa_device_ctrl_controller.log"
if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  kill "$OLD_PID" 2>/dev/null
fi
nohup python3 "$ROOT/tools/runtime_control_server.py" "$RUN_ID" "$PORT" >"$LOG_FILE" 2>&1 &
PID=$!
printf '%s\n' "$PID" > "$PID_FILE"
sleep 1
curl -fsS "http://127.0.0.1:$PORT/health"
printf '\nCONTROLLER_CHECK_RC=%s\n' "$?"
printf 'CONTROLLER_PID=%s\n' "$PID"
printf 'CONTROLLER_URL=http://127.0.0.1:%s\n' "$PORT"
