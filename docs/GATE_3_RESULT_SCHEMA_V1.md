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
- `EXEC_PRIVATE`
- `EXEC_SHARED`
- `SCRIPT_BASH`
- `SCRIPT_PYTHON`
- `PROCESS_SPAWN`

## Capability Result Rule

Each capability must contain a status and execution scope in the producer output.

Allowed capability statuses:

```text
PASS
ERROR
```

`PASS` requires successful live verification. `ERROR`, `UNKNOWN`, `WARNING`, `BLOCKED`, missing, or malformed results cannot produce a GREEN G3 gate.

## Gate Result Rule

G3 is `GREEN` only when all five capabilities are `PASS`, the producer returns zero, the G2 prerequisite is GREEN, and the source commit matches the current repository `HEAD` exactly.

## Evidence

Canonical gate evidence:

```text
artifacts/gates/g3/execution-capability.txt
```

Schema identifier:

```text
g3-execution-capability.v1
```

## Verification State

Schema: LOCKED
Implementation: PRESENT
Verification: REQUIRED_ON_CURRENT_ENVIRONMENT
