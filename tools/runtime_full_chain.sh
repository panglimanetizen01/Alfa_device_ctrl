#!/usr/bin/env bash
# Full explicit Gate 6-19 chain. No newest-artifact selection.
set -u

main() {
    local ROOT RUN_ID GATE5_AUTH RC FINAL
    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    RUN_ID=${1:-run_20260823_171233_30513}
    GATE5_AUTH="$ROOT/artifacts/pipeline/$RUN_ID/gate5/authorizations/gate5-self-test.txt"
    printf '%s\n' '=== ALFA DEVICE CTRL FULL RUNTIME CHAIN GATE 6-19 ==='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    if [ ! -f "$GATE5_AUTH" ]; then
        printf '%s\n' 'FULL_CHAIN_STATUS=BLOCKED'
        printf '%s\n' 'FULL_CHAIN_REASON=Gate 5 authorization artifact missing'
        return 0
    fi
    bash "$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_session.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_task.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_action.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_workflow.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_orchestrator.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_kernel.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_command.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_command_validate.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_command_policy.sh" "$RUN_ID"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_stage.sh" gate16 "$RUN_ID" "$ROOT/artifacts/pipeline/$RUN_ID/gate15/policy.txt" "$GATE5_AUTH" "$ROOT/artifacts/pipeline/$RUN_ID/gate16/authorization.txt"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_stage.sh" gate17 "$RUN_ID" "$ROOT/artifacts/pipeline/$RUN_ID/gate16/authorization.txt" "$ROOT/artifacts/pipeline/$RUN_ID/gate13/command.txt" "$ROOT/artifacts/pipeline/$RUN_ID/gate17/execution.txt"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$ROOT/artifacts/pipeline/$RUN_ID/gate17/execution.txt" "" "$ROOT/artifacts/pipeline/$RUN_ID/gate18/result.txt"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    bash "$ROOT/tools/runtime_stage.sh" gate19 "$RUN_ID" "$ROOT/artifacts/pipeline/$RUN_ID/gate18/result.txt" "" "$ROOT/artifacts/pipeline/$RUN_ID/gate19/consumer.txt"; RC=$?; [ "$RC" -eq 0 ] || return "$RC"
    FINAL=$(grep '^consume_status=' "$ROOT/artifacts/pipeline/$RUN_ID/gate19/consumer.txt" 2>/dev/null | sed -n '1p' | cut -d= -f2-)
    printf '%s\n' "FULL_CHAIN_STATUS=$([ "$FINAL" = 'ACCEPTED' ] && printf '%s' 'PASS' || printf '%s' 'BLOCKED')"
    printf '%s\n' "consume_status=${FINAL:-UNKNOWN}"
    printf '%s\n' "result_artifact=$ROOT/artifacts/pipeline/$RUN_ID/gate18/result.txt"
    printf '%s\n' "consumer_artifact=$ROOT/artifacts/pipeline/$RUN_ID/gate19/consumer.txt"
    return 0
}

main "$@"
