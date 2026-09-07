# Gate 3 — Execution Capability Engine V1

## Status

**Decision:** LOCKED verification contract

G3 proves that the current execution environment can perform the capabilities required by the runtime foundation. G3 is environment evidence, not Linux-distro acceptance.

## Prerequisite

G3 may be GREEN only when G2 evidence is GREEN and its `source_commit` exactly matches the current repository `HEAD`.

## Capabilities

The following five capabilities are mandatory and independently tested:

- `EXEC_PRIVATE` — create, mark executable, and execute a private temporary script.
- `EXEC_SHARED` — create, mark executable, and execute a temporary script in the configured shared-storage root.
- `SCRIPT_BASH` — execute a Bash script and verify its output.
- `SCRIPT_PYTHON` — execute a Python 3 script and verify its output.
- `PROCESS_SPAWN` — create a subprocess and verify its output.

## Rules

1. Every capability must be tested by an actual execution attempt.
2. A capability is `PASS` only when its execution result is verified.
3. Any capability other than `PASS` makes G3 `RED`.
4. The producer must return a non-zero status when G3 is not fully `PASS`.
5. `UNKNOWN`, missing, malformed, or stale evidence is not `PASS`.
6. Evidence is valid only for the current execution environment and exact source commit.
7. G3 evidence must identify the G2 evidence consumed by the gate.
8. The shared-storage root is configurable through `ALFA_EXEC_SHARED_ROOT`; the default is `/storage/emulated/0` for Android/Termux verification.
9. G3 does not claim Android application runtime, PRoot, guest rootfs, or distro support.

## Evidence Contract

Evidence path:

```text
artifacts/gates/g3/execution-capability.txt
```

Schema:

```text
g3-execution-capability.v1
```

Required fields:

```text
schema_version
gate
gate_status
source_commit
contract_identity
consumed_artifacts
execution_path
shared_root
exec_private
exec_shared
script_bash
script_python
process_spawn
created_at
```

## Negative Requirements

G3 must be `RED` when any of the following occurs:

- G2 evidence is missing, stale, malformed, or not GREEN;
- the current source commit cannot be determined exactly;
- any capability returns `ERROR` or is absent;
- the capability producer exits zero despite a failed capability;
- the evidence cannot be written or does not bind to the current source commit.

## Implementation Boundary

`tools/execution_capability.sh` performs only the environment capability tests. A separate G3 gate validator is responsible for provenance, prerequisite validation, all-capability PASS enforcement, evidence generation, and fail-closed status.

## Initial State Resolution

The historical statement `Implementation: NOT STARTED` is superseded by this locked verification contract. Implementation is considered present only after the G3 validator and fresh current-environment evidence prove the rules above.
