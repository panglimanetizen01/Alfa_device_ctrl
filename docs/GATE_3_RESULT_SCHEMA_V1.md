# Gate 3 — Execution Capability Result Schema V1

## Required Fields

- `schema_version`
- `gate`
- `gate_status`
- `source_commit`
- `contract_identity`
- `consumed_artifacts`
- `created_at`
- `execution_path`
- `shared_root`
- `exec_private`
- `shared_storage_io`
- `script_bash`
- `script_python`
- `process_spawn`
- `producer_rc`
- `producer_status`

## Capability Result Rule

Each capability must contain a status and execution scope in the producer output.

Allowed capability statuses:

```text
PASS
ERROR
```

`PASS` requires successful live verification. `ERROR`, `UNKNOWN`, `WARNING`, `BLOCKED`, missing, or malformed results cannot produce a GREEN G3 gate.

`exec_private` proves direct execution only in the current private execution-capable workspace. `shared_storage_io` proves shared-storage file access only; it is explicitly not an executable-code test.

## Gate Result Rule

G3 is `GREEN` only when all five capabilities are `PASS`, the producer returns zero, the producer status is `PASS`, the G2 prerequisite is GREEN, and the source commit matches the current repository `HEAD` exactly.

## Evidence

Canonical gate evidence:

```text
artifacts/gates/g3/execution-capability.txt
```

Schema identifier:

```text
g3-execution-capability.v2
```

## Verification State

Schema: LOCKED
Implementation: PRESENT
Verification: REQUIRED_ON_CURRENT_ENVIRONMENT
