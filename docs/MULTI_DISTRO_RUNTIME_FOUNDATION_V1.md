# Alfa Device Ctrl — Multi-Distro Linux Runtime Foundation V1

## Status

**Decision:** LOCKED architectural direction

**Scope:** resolve the mismatch between documented architecture, gate semantics, implementation, APK behavior, and runtime evidence.

**Primary acceptance target after G1–G19:** Debian

## 1. Problem Being Corrected

The repository historically mixed several different claims:

- Android device-control architecture
- Ubuntu UserLAnd as a development/execution environment
- PRoot runtime implementation
- Ubuntu-specific UI/install flow
- sequential Gate 5–19 evidence
- CI fixture verification

These are not equivalent claims. This specification separates them.

## 2. Product Claim

Alfa Device Ctrl provides a **distribution-neutral, rootless Linux userspace runtime foundation on Android**.

A Linux distribution is a guest rootfs. The Android application owns the runtime lifecycle. The Linux guest shares the Android kernel and runs through the packaged rootless execution engine.

The runtime foundation must not require UserLAnd, Termux, or another Linux host to exist on the target device after APK installation.

## 3. Distribution-Neutral Contract

The execution engine accepts a runtime descriptor rather than a distro-specific execution branch.

Required descriptor fields:

```text
runtime_id
family
version
architecture
archive_format
rootfs_uri
rootfs_sha256
shell_path
acceptance_status
```

Required invariant:

```text
same engine + different verified rootfs = different Linux guest
```

The engine may reject a rootfs for generic runtime incompatibility, but it must not contain distro-specific execution semantics merely to make one distro work.

## 4. Rootfs Trust Contract

A rootfs is eligible for installation only when:

1. the source URI is attributable;
2. the expected SHA-256 is present;
3. the downloaded archive matches the expected SHA-256;
4. archive extraction succeeds without unsafe path traversal;
5. the rootfs contains the required shell/runtime structure;
6. the PRoot smoke test succeeds;
7. publication is atomic;
8. READY evidence is written only after the previous conditions pass.

## 5. PRoot Contract

PRoot is the Linux userspace execution mechanism, not a distribution.

The engine must be packaged in the APK and verified independently from the guest rootfs.

The packaged engine must be:

- attributable to a pinned source revision;
- compatible with the target Android ABI;
- checksum verified;
- executable from the application's native library location;
- tested with a real guest rootfs.

## 6. Session Contract

A runtime session is READY only after a real command executes inside the guest rootfs.

Minimum proof:

```text
command=pwd
returncode=0
stdout=<guest path>
```

Linux Userspace Acceptance extends this to:

```text
id
uname
pwd
cat /etc/os-release
/bin/sh -c 'printf ...'
```

The evidence must prove that these commands ran inside the guest userspace rather than the Android host shell.

## 7. Evidence / Provenance Contract

Every gate artifact must identify:

```text
schema_version
 gate
 pipeline_run_id
 source_commit
 contract_identity
 consumed_artifacts
 created_at
 execution_path
 gate_status
```

Evidence from a different source commit is stale evidence.

Evidence without an upstream artifact is incomplete evidence.

Evidence marked UNKNOWN is not PASS.

## 8. Sequential Gate Contract

The gate state machine is:

```text
G1 GREEN → G2 GREEN → G3 GREEN → ... → G19 GREEN
```

For every gate `Gn`:

```text
input status must be GREEN
input provenance must match
contract version must match
required artifacts must exist
implementation must satisfy the contract
fresh evidence must be generated
```

If any condition fails:

```text
Gn = RED/BLOCKED/UNKNOWN
stop
investigate root cause
patch canonical source
re-run Gn
only then continue
```

## 9. G1 Contract

G1 is the foundation contract gate.

G1 is GREEN only if all of the following are proven against one source commit:

1. `docs/ARCHITECTURE.md` identifies Alfa as a multi-distro Linux runtime foundation.
2. The architecture separates Android host, runtime engine, and guest distro layers.
3. The runtime model is distribution-neutral.
4. Debian, Ubuntu, Alpine, Kali, and generic rootfs are represented as acceptance targets rather than execution engines.
5. Gate sequencing is explicitly fail-closed and sequential.
6. Provenance rules forbid stale or cross-run evidence.
7. A machine-checkable G1 contract validator executes successfully.
8. G1 evidence records the exact source commit and contract identity.

G1 does **not** claim that Debian, Ubuntu, Alpine, Kali, or any APK runtime is already usable.

## 10. Gate 2–19 Boundary

G2–G19 retain their individual responsibilities but must be reconciled against this foundation.

Particularly:

- G3/G4 describe capabilities and environment evidence, not Linux distro support.
- G5/G6 establish decision/authorization/bootstrap foundations.
- G7–G12 must represent actual task/action/workflow/orchestrator/kernel/command semantics; propagation-only placeholders cannot be declared implementation-complete.
- G13 creates the concrete `pwd` request evidence.
- G14–G16 validate, authorize, and policy-gate the request.
- G17 executes the command in the real runtime.
- G18 normalizes execution evidence.
- G19 accepts the evidence chain.

## 11. Linux Userspace Acceptance

This is a separate milestone after G19.

### D1 — Debian

Must demonstrate a real Debian userspace on the target Android device.

### D2 — Ubuntu

Must demonstrate a real Ubuntu userspace using the same runtime foundation.

### D3 — Alpine

Must demonstrate a real Alpine userspace using the same runtime foundation.

### D4 — Kali

Must demonstrate a real Kali userspace using the same runtime foundation.

### D5 — Generic Rootfs

Must demonstrate that a compatible externally supplied rootfs can be installed and executed using the same descriptor/engine contract without distro-specific code paths.

## 12. Release Gate

No APK is a release candidate until:

```text
G1..G19 = GREEN
AND
D1 Debian = GREEN
```

Subsequent distro support is additive:

```text
D2 Ubuntu
D3 Alpine
D4 Kali
D5 Generic Rootfs
```

No UI label, fixture, CI-only rootfs, or mock terminal can substitute for device runtime evidence.
