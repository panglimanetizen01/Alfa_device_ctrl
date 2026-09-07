# Gate 3 — Execution Capability Engine V1

## Status

**Decision:** LOCKED verification contract

G3 proves that the current execution environment can perform the execution and process capabilities required by the runtime foundation. G3 is environment evidence, not Linux-distro acceptance.

## Execution Boundary Decision

G3 separates **execution-capable private staging** from **shared-storage access**.

Android shared storage is a user-facing/shared data boundary and must not be treated as the runtime's executable staging area. Linux `execve()` can reject direct execution from a `noexec` filesystem even when a file has execute permission. Android security guidance also treats external/shared storage as untrusted and explicitly warns against storing executables there before dynamic loading. Therefore G3 must not require direct execution from `/storage/emulated/0`.

The runtime execution boundary is an app-private/runtime-controlled staging area. In the Android product, packaged native engine code is supplied by the APK/native library boundary and writable runtime state is staged separately. G3 does not claim to prove the Android application's native-library execution itself; that belongs to later Android/runtime validation gates.

## Prerequisite

G3 may be GREEN only when G2 evidence is GREEN and its `source_commit` exactly matches the current repository `HEAD`.

## Capabilities

The following five capabilities are mandatory and independently tested:

- `EXEC_PRIVATE` — create, mark executable, and execute a private temporary script in the current execution-capable workspace.
- `SHARED_STORAGE_IO` — create, write, read, verify, and delete a temporary file in the configured shared-storage root. This proves storage bridge I/O, not executable-code loading.
- `SCRIPT_BASH` — execute a Bash script and verify its output.
- `SCRIPT_PYTHON` — execute a Python 3 script and verify its output.
- `PROCESS_SPAWN` — create a subprocess and verify its output and exit status.

## Rules

1. Every capability must be tested by an actual operation.
2. `EXEC_PRIVATE` is the only G3 capability that directly tests execution of a newly-created file.
3. `SHARED_STORAGE_IO` must never require `chmod +x` or direct execution from shared storage.
4. A capability is `PASS` only when its result is independently verified.
5. Any capability other than `PASS` makes G3 `RED`.
6. The producer must return a non-zero status when G3 is not fully `PASS`.
7. `UNKNOWN`, missing, malformed, or stale evidence is not `PASS`.
8. Evidence is valid only for the current execution environment and exact source commit.
9. G3 evidence must identify the G2 evidence consumed by the gate.
10. `ALFA_EXEC_SHARED_ROOT` selects the shared-storage I/O root; the default is `/storage/emulated/0` for Android/Termux verification.
11. G3 does not claim Android application runtime, packaged native-library execution, PRoot, guest rootfs, or distro support.

## Evidence Contract

Evidence path:

```text
artifacts/gates/g3/execution-capability.txt
```

Schema:

```text
g3-execution-capability.v2
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
shared_storage_io
script_bash
script_python
process_spawn
producer_rc
producer_status
created_at
```

## Negative Requirements

G3 must be `RED` when any of the following occurs:

- G2 evidence is missing, stale, malformed, or not GREEN;
- the current source commit cannot be determined exactly;
- any capability returns `ERROR` or is absent;
- shared storage is only executable but cannot be reliably read/written/verified;
- the capability producer exits zero despite a failed capability;
- the evidence cannot be written or does not bind to the current source commit.

## Implementation Boundary

`tools/execution_capability.sh` performs only the environment capability tests. A separate G3 gate validator is responsible for provenance, prerequisite validation, all-capability PASS enforcement, evidence generation, and fail-closed status.

## Objective Evidence Boundary

A G3 PASS is objective evidence of the five explicitly tested operations in the current verification environment. It is not evidence that the Android APK can execute a guest Linux runtime. Product-level execution evidence must later come from the Android/runtime gates and final Linux userspace acceptance.

## Initial State Resolution

The historical statement `Implementation: NOT STARTED` is superseded by this locked verification contract. Implementation is considered present only after the G3 validator and fresh current-environment evidence prove the rules above.
