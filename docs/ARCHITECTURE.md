# Alfa Device Ctrl — Architecture

## 1. Purpose

Alfa Device Ctrl is a native Android device-control project.

The project is designed from the beginning around the real Android device and
shared storage environment, without depending on Alpine Linux.

## 2. Master / Execution Separation

MASTER:
`/storage/emulated/0/Alfa_device_ctrl`

EXECUTION ENVIRONMENT:
Ubuntu UserLAnd

The execution environment is not the project source of truth.

## 3. Core Design Principles

1. Android device is the primary platform.
2. Master source remains on Android shared storage.
3. No Alpine dependency.
4. No hidden workspace or mirror repository.
5. Every executable component must have attributable source.
6. Every build artifact must be traceable to source.
7. Runtime behavior must be traceable to a known component.
8. Device capabilities must never be inferred merely from host/runtime visibility.
9. Permissions and Android framework boundaries must be explicit.
10. Auditability is a first-class architectural requirement.

## 4. Initial Component Model

alfa_device_ctrl
├── device-control core
├── Android integration layer
├── permission/capability layer
├── storage/device bridge
├── command interface
└── verification/test layer

These are architectural boundaries only.
No implementation is claimed to exist yet.

## 5. Traceability Chain

SOURCE
  ↓
BUILD
  ↓
PACKAGE / ARTIFACT
  ↓
INSTALLATION
  ↓
RUNTIME PROCESS
  ↓
DEVICE OPERATION
  ↓
VERIFICATION

A capability is considered implemented only when this chain can be
demonstrated with attributable evidence.

## 6. Initial State

Architecture: DEFINED
Implementation: NOT STARTED
Build: NOT STARTED
Runtime: NOT STARTED
Verification: NOT STARTED
