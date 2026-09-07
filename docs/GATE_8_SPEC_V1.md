# Gate 8 — Runtime Task Engine V1

## Purpose

Gate 8 constructs and admits a concrete runtime task only from a verified Gate 7 session. It does not execute the guest command; execution remains downstream at Gate 17.

## Rules

1. Task construction requires `gate7_status=PASS` and `session_status=PASS`.
2. The Gate 7 artifact must be bound to the explicit pipeline run and current Git HEAD.
3. The task artifact must carry pipeline/source/Gate4/profile/session/request provenance.
4. The task definition must identify a concrete task name and target session.
5. Gate 8 must not execute the guest command and must not change capability status.
6. Task execution is explicitly deferred to the downstream execution chain; `execution_status=DEFERRED` is not execution success.
7. Missing, stale, malformed, cross-run, or UNKNOWN provenance is BLOCKED.
8. No artifact may be selected by directory order, timestamps, `ls -1t`, or `head -1`.

## Evidence contract

A PASS artifact uses schema `gate8-runtime-task.v1` and records the exact input Gate 7 artifact, current source commit, task identity, target session, execution disposition, timestamp, and execution path.

## Status

Specification: LOCKED
Implementation: REQUIRED
Verification: REQUIRED

## Scope boundary

G8 proves task construction/admission from a valid runtime session. It does not claim Android APK runtime execution, guest command execution, distro acceptance, or Linux userspace acceptance. Those claims remain downstream.

## Verification basis

The contract follows the project's fail-closed provenance model and NASA verification practice: test configuration and requirements are identified, actual results are compared against expected criteria, discrepancies are rejected rather than converted to PASS, and objective evidence is recorded for repeatable verification.
