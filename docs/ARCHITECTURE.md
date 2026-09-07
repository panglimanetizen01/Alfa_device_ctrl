# Alfa Device Ctrl — Multi-Distro Linux Runtime Foundation

## 1. Product Identity

Alfa Device Ctrl is a native Android application whose runtime purpose is to provide a **rootless Linux userspace foundation on the real Android device**.

The foundation is distribution-neutral. Ubuntu is not the architecture, Alpine is not the architecture, and no single Linux distribution is the source of truth. Distributions are guest userspaces consumed by a common Android-hosted runtime engine.

The first Linux userspace acceptance target is **Debian**. Ubuntu, Alpine, Kali, and additional compatible distributions are subsequent acceptance targets. A distribution is not considered supported because its name appears in the UI or registry; it is supported only after the complete source → package → installation → runtime → command → evidence chain passes.

## 2. Master / Execution Separation

MASTER SOURCE:
`/storage/emulated/0/Alfa_device_ctrl_HOST/Alfa_device_ctrl`

CANONICAL REPOSITORY:
`panglimanetizen01/Alfa_device_ctrl`

OPERATOR EXECUTION ENVIRONMENT:
Termux on the real Android device.

Historical Ubuntu UserLAnd is an execution aid only. It is not the product runtime and must not be required for Alfa to execute a Linux guest userspace.

## 3. Runtime Layers

```text
Android device
└── Alfa Device Ctrl APK
    ├── Android host integration
    │   ├── lifecycle / permissions
    │   ├── shared-storage bridge
    │   └── app-private runtime vault
    │
    ├── Linux runtime foundation
    │   ├── packaged ARM64 PRoot engine
    │   ├── runtime descriptor / registry
    │   ├── verified rootfs installer
    │   ├── runtime session / PTY
    │   └── evidence + provenance
    │
    └── Linux guest userspaces
        ├── Debian       ← first acceptance target
        ├── Ubuntu
        ├── Alpine
        ├── Kali
        └── Generic compatible rootfs
```

The Android kernel remains the kernel. PRoot provides userspace path/syscall mediation and does not replace the kernel or create a virtual machine. The primary PRoot documentation describes it as a user-space implementation of chroot, mount-bind, and binfmt_misc that relies on ptrace.

## 4. Non-Negotiable Boundaries

1. The Android device is the product platform.
2. Linux distributions are guest userspaces, not host environments.
3. The runtime engine is independent of any specific distribution.
4. A distro descriptor contains metadata and immutable artifact identity; it does not contain execution logic.
5. Rootfs archives must be cryptographically verified before publication.
6. The packaged PRoot engine is independently verified and immutable at runtime.
7. Runtime installation is transactional: stage → validate → smoke test → publish → evidence.
8. A runtime session is not considered ready until a real guest command executes successfully.
9. UI state must be derived from runtime evidence; it must never manufacture a READY state.
10. A gate PASS is valid only for the exact source commit and exact evidence artifacts that produced it.
11. UNKNOWN remains UNKNOWN. Missing evidence is not PASS.
12. UserLAnd, Termux, or another Linux host environment may be used for development/testing, but none may be silently substituted for the APK's product runtime.

## 5. Generic Runtime Contract

Every Linux guest runtime is represented by a descriptor containing at minimum:

- stable runtime ID
- distribution family
- distribution version
- architecture
- rootfs archive format
- immutable rootfs SHA-256
- rootfs source URI
- expected shell path
- acceptance status

The runtime engine consumes the descriptor. It does not branch on `ubuntu`, `debian`, `alpine`, or `kali` to implement different execution mechanisms.

## 6. Runtime Lifecycle

```text
DISCOVER
  ↓
VERIFY ARTIFACT IDENTITY
  ↓
STAGE ROOTFS
  ↓
VALIDATE ROOTFS
  ↓
PRoot SMOKE TEST
  ↓
ATOMIC PUBLISH
  ↓
START PTY SESSION
  ↓
EXECUTE REAL GUEST COMMAND
  ↓
CAPTURE EVIDENCE
  ↓
MARK RUNTIME READY
```

`READY` is an evidence-backed state, not an installer-side assumption.

## 7. Gate Model

The verification chain is strictly sequential:

```text
G1 → G2 → G3 → ... → G19
```

Rules:

- G(n+1) cannot be executed as a PASS candidate until G(n) is GREEN.
- A BLOCKED, RED, UNKNOWN, stale, incomplete, or wrong-commit artifact stops the chain.
- Historical evidence from another source commit cannot satisfy the current gate.
- Each gate must emit attributable evidence containing source commit, run identity, contract/version identity, status, and the artifacts it consumed.
- The chain may resume only from the first unresolved gate after its root cause is corrected and re-verified.

## 8. Gate Responsibilities

| Gate | Responsibility |
|---|---|
| G1 | Foundation contract: architecture, generic runtime model, gate sequencing, provenance rules, and executable contract verification |
| G2 | Source/build boundary and canonical project integrity |
| G3 | Android/host execution capability evidence |
| G4 | Environment contract and capability normalization |
| G5 | Runtime decision / authorization contract |
| G6 | Runtime bootstrap and session foundation |
| G7 | Task construction |
| G8 | Action construction |
| G9 | Workflow construction |
| G10 | Orchestrator execution contract |
| G11 | Kernel/runtime command preparation |
| G12 | Command request generation |
| G13 | Concrete `pwd` command request evidence |
| G14 | Command validation |
| G15 | Command policy |
| G16 | Execution authorization |
| G17 | Real command execution |
| G18 | Execution result normalization |
| G19 | Evidence consumption / terminal acceptance |

A gate's name does not imply that its implementation exists. The implementation and its evidence must satisfy the contract before the gate can be GREEN.

## 9. Linux Userspace Acceptance

After G19 is GREEN, distribution acceptance is a separate milestone and does not retroactively change the gate chain.

Acceptance order:

1. Debian
2. Ubuntu
3. Alpine
4. Kali
5. Generic compatible rootfs

Each distribution must demonstrate on the real Android device:

- APK installation from the attributable build
- rootfs acquisition and checksum verification
- successful runtime installation
- real Linux guest process execution
- `id`
- `uname`
- `pwd`
- `/etc/os-release` identity
- `/bin/sh` execution
- filesystem access inside the guest rootfs
- process evidence
- clean session shutdown
- persisted evidence tied to APK/source/runtime/rootfs identities

Opening the Android Activity, displaying a terminal widget, starting a PRoot process, or showing a hard-coded distro name is insufficient.

## 10. Storage Model

Private runtime state belongs inside the app-private runtime vault. User-exportable data belongs in Android shared storage through Android-supported storage APIs. The Linux rootfs itself is an application-managed runtime artifact and must not depend on a UserLAnd home directory.

Android's scoped-storage model distinguishes app-specific storage from shared storage; files intended for other apps/users should use the appropriate shared-storage mechanism.

## 11. Current State Declaration

Architecture: **REDEFINED — MULTI-DISTRO LINUX RUNTIME FOUNDATION**

G1–G19: **NOT PROVEN until re-run under this contract**

Linux Userspace Acceptance: **NOT STARTED**

First acceptance target: **Debian**

No APK release or installation is authorized until the sequential gate chain and the required distribution acceptance evidence are GREEN.
