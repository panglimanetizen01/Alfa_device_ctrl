#!/usr/bin/env bash
# Serve one explicit Gate 5-19 current run to the Android app over localhost.
# Usage: bash tools/runtime_android_bridge.sh <pipeline_run_id>
set +e
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-}"
SRC="$ROOT/artifacts/pipeline/$RUN_ID"
BRIDGE_ROOT="$HOME/.alfa_device_ctrl_bridge/$RUN_ID"
STATUS="$BRIDGE_ROOT/status.txt"
PID_FILE="$HOME/.alfa_device_ctrl_bridge/server.pid"

if [ -z "$RUN_ID" ]; then
  printf 'BRIDGE_STATUS=BLOCKED\n'
  printf 'BRIDGE_REASON=explicit pipeline_run_id is required\n'
elif [ ! -d "$SRC" ]; then
  printf 'BRIDGE_STATUS=BLOCKED\n'
  printf 'BRIDGE_REASON=run directory is missing\n'
  printf 'SOURCE=%s\n' "$SRC"
else
  mkdir -p "$BRIDGE_ROOT"
  {
    printf 'BRIDGE_STATUS=READY\n'
    printf 'PIPELINE_RUN_ID=%s\n' "$RUN_ID"
    printf 'SOURCE=%s\n' "$SRC"
    printf 'MODE=READ_ONLY\n'
    printf '\nGATE_5\n'
    for pair in \
      'gate5/decisions/gate5-self-test.txt:decision' \
      'gate5/authorizations/gate5-self-test.txt:authorization_status' \
      'gate5/executions/gate5-self-test.txt:execution_status' \
      'gate5/executions/gate5-self-test.txt:result_status'
    do
      FILE="${pair%%:*}"; KEY="${pair##*:}"
      VALUE=$(awk -F= -v k="$KEY" '$1 == k {print substr($0, index($0,"=")+1); found=1} END {if (!found) print "MISSING"}' "$SRC/$FILE" 2>/dev/null)
      printf '%s=%s\n' "$KEY" "$VALUE"
    done
    printf '\nGATE_6_19\n'
    for gate_file in \
      '6:bootstrap.txt' '7:session.txt' '8:task.txt' '9:action.txt' \
      '10:workflow.txt' '11:orchestrator.txt' '12:kernel.txt' \
      '13:command.txt' '14:validation.txt' '15:policy.txt' \
      '16:authorization.txt' '17:execution.txt' '18:result.txt' '19:consumer.txt'
    do
      GATE="${gate_file%%:*}"; FILE="${gate_file##*:}"
      VALUE=$(awk -F= '$1 == "gate_status" {print substr($0, index($0,"=")+1); found=1} END {if (!found) print "MISSING"}' "$SRC/gate$GATE/$FILE" 2>/dev/null)
      printf 'gate%s=%s\n' "$GATE" "$VALUE"
    done
    VALUE=$(awk -F= '$1 == "consume_status" {print substr($0, index($0,"=")+1); found=1} END {if (!found) print "MISSING"}' "$SRC/gate19/consumer.txt" 2>/dev/null)
    printf 'consume_status=%s\n' "$VALUE"
  } > "$STATUS"
  if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    kill "$OLD_PID" 2>/dev/null
  fi
  python3 -m http.server 8090 --bind 0.0.0.0 --directory "$BRIDGE_ROOT" >/tmp/alfa_android_bridge.log 2>&1 &
  printf '%s' "$!" > "$PID_FILE"
  printf 'BRIDGE_STATUS=READY\n'
  printf 'PIPELINE_RUN_ID=%s\n' "$RUN_ID"
  printf 'BRIDGE_URL=http://127.0.0.1:8090/status.txt\n'
  printf 'STATUS_FILE=%s\n' "$STATUS"
fi
