# Alfa Multi-Distro Runtime Design

## Goal
Make Alfa Device Ctrl a verified multi-distro Linux runtime manager on stock Android/ARM64 rather than a Debian-only terminal.

## Scope
Initial supported profiles: Debian Bookworm, Ubuntu Base 24.04.4 ARM64, Alpine Mini RootFS 3.24.1 aarch64, and Kali NetHunter Rootless minimal ARM64. A profile is installable only when its artifact identity is pinned and its rootfs contract passes verification.

## Architecture
`RuntimeProfile` becomes an immutable value object. `RuntimeRegistry` owns the built-in profiles and lookup. UI and session code consume a selected profile instead of static Debian constants. Installation remains artifact-first: download -> pinned identity verification -> extraction -> rootfs validation -> READY. Session startup consumes the selected profile's runtime id, rootfs and prompt/environment contract.

## Profile Contract
Each profile carries: id, display name, version, architecture, rootfs URL, SHA-256 identity, archive format, shell, package manager, prompt prefix/contract, environment, required rootfs paths, PRoot arguments, and capability declarations.

## Security
No profile is accepted with an unpinned artifact identity. PRoot loader verification remains mandatory. Kali Rootless capabilities are explicitly limited to what stock Android + PRoot can provide; kernel-dependent features are not claimed as available.

## UI Contract
The runtime selector must display actual registered profiles and their installation state. Selecting a profile must change the runtime id used by install, status, terminal, htop/network/process telemetry, session startup and prompt verification. Renaming Debian is not considered multi-distro support.

## Acceptance
- Registry contains four profiles with unique ids.
- Debian regression remains green.
- Ubuntu and Kali artifact identities are pinned to official sources.
- Alpine is not marked installable until its official SHA-256 is captured and pinned.
- No production path depends on a global Debian singleton.
- Prompt verification is runtime-id aware.
- Full unit test suite passes.
- CI passes on the canonical commit.
- APK installs and the selected Linux rootfs reaches an interactive PTY on the ARM64 DUT.
