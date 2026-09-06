# Gate 5 — Runtime Decision, Authorization, and Execution Enforcement V1

## Purpose

Gate 5 consumes one explicit, VALID Gate 4 Environment Contract and binds a runtime request, policy decision, authorization, and execution result to the same pipeline identity.

## Required identity

Every request, decision, authorization, and execution artifact must carry matching values for `request_id`, `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, `profile_sha256`, and `policy_version`. Gate 4 is loaded only from `artifacts/pipeline/<pipeline_run_id>/gate4/environment_contract.txt`; no newest-file, directory-order, historical-fallback, or `ls ... | head` selection is allowed.

## Decision

The decision artifact must contain `schema_version`, `decision_id`, `request_id`, `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, `profile_sha256`, `policy_version`, `decision`, `decision_reason`, and `created_at`. The only decision values are `ALLOW` and `DENY`.

Gate 5 V1 allows only `action=pwd`, `resource=runtime`, and `context=purpose=self-test`. Missing, malformed, stale, or mismatched Gate 4 evidence produces `decision=DENY`.

## Authorization

Authorization is a separate step. It succeeds only when the exact request and decision have matching identity, a current VALID Gate 4 contract is present, timestamps are fresh, and `decision=ALLOW`. Otherwise it produces `authorization_status=DENIED`.

## Execution enforcement

`tools/runtime_execution.sh` requires explicit request, decision, authorization, and output paths. It never discovers an artifact by recency. It executes only the fixed command `pwd` after rechecking identity, Gate 4, freshness, decision, and authorization. A failure produces `execution_status=BLOCKED`, `result_status=DENIED`, and `command_result=NOT_EXECUTED`.
