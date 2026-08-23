# Gate 3 — Execution Capability Result Schema V1

## Required Fields

- timestamp
- execution_path
- EXEC_PRIVATE
- EXEC_SHARED
- SCRIPT_BASH
- SCRIPT_PYTHON
- PROCESS_SPAWN

## Result Rule

Each capability must contain:

- status
- verification
- execution scope

Allowed status:

PASS
WARNING
ERROR
BLOCKED
UNKNOWN

PASS requires successful live verification.

UNKNOWN must remain UNKNOWN.

## Initial State

Schema: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED
