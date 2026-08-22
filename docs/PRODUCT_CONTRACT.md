# Alfa Device Ctrl — Product Contract

## 1. Product Identity

Name:
`alfa_device_ctrl`

Platform:
Android device

Master:
`/storage/emulated/0/Alfa_device_ctrl`

Execution environment:
Ubuntu UserLAnd

## 2. Primary Objective

Provide a controlled, explicit, and auditable interface for device-level
operations from Android.

The product must distinguish clearly between:

- capability that actually belongs to Alfa Device Ctrl,
- capability provided by Android,
- capability provided by the execution environment,
- capability provided by external applications or services.

## 3. Non-Goals

Alfa Device Ctrl must NOT:

- depend on Alpine Linux,
- require a hidden Linux workspace,
- use an alternate project mirror,
- claim host/runtime capabilities as Alfa capabilities,
- treat filesystem visibility as proof of Android API access,
- hide permission failures,
- silently fall back to unrelated external components.

## 4. Capability Contract

Every device capability must define:

1. Capability ID
2. Required Android permission(s)
3. Android API/framework boundary
4. Alfa-owned implementation component
5. Source location
6. Build artifact
7. Runtime component
8. Verification procedure
9. Expected success result
10. Expected failure result

## 5. Evidence Contract

A capability is GREEN only when:

SOURCE
→ BUILD
→ ARTIFACT
→ INSTALLATION
→ RUNTIME
→ OPERATION
→ VERIFICATION

is attributable and reproducible.

If any required link is missing:

Status = BLOCKED

The product must never convert missing evidence into an assumption.

## 6. Error Classification

### GREEN — PASS

The operation completed and the expected result was independently verified.

### YELLOW — WARNING

The operation completed but has a known limitation, degraded condition,
non-critical anomaly, or incomplete secondary evidence.

### RED — ERROR

The requested operation failed, the implementation violated its contract,
or a critical runtime/build/source condition is invalid.

### BLOCKED

The operation cannot be legitimately evaluated because required evidence,
permission, artifact, or prerequisite is missing.

## 7. Storage Principle

The project Master remains on Android shared storage:

`/storage/emulated/0/Alfa_device_ctrl`

Ubuntu UserLAnd is an execution environment only.

No project source may be intentionally maintained as a second authoritative
copy inside `/home/userland`.

## 8. Auditability

Every implementation change must be traceable through Git.

Every release/build must be attributable to:

- Git commit
- source tree
- build configuration
- resulting artifact
- runtime version

## 9. Initial Product Status

Contract: DEFINED
Implementation: NOT STARTED
Build: NOT STARTED
Runtime: NOT STARTED
Verification: NOT STARTED
