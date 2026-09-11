# Alfa Network Independence Architecture

## Architectural invariant

**Alfa local execution must tolerate network disruption. Network disruption may degrade network capabilities, but must not destroy the local terminal execution plane.**

This is an architectural invariant derived from the resilience principles used in NASA Delay/Disruption Tolerant Networking (DTN): communication links may be intermittent, unavailable, delayed, or changed without making the entire application non-operational.

## Failure-domain separation

### Local execution plane

The following path is local and must not require Wi-Fi, cellular service, DNS, VPN, proxy, tunnel, MCP, or a remote provider:

`TerminalView → TerminalSession → JNI PTY → PRoot → loader → rootfs → shell → prompt → READY`

A network outage must not prevent creation of the PTY, execution of the selected local runtime, shell startup, prompt detection, or local terminal interaction.

### Network plane

The following capabilities may legitimately degrade when connectivity is unavailable:

- network-interface and route evidence;
- DNS-dependent operations;
- runtime artifact download/install;
- MCP remote transport;
- tunnel/remote-control transport;
- provider-dependent services.

Their failure state must be explicit (`BLOCKED`, `DEGRADED`, or equivalent) and must not be represented as failure of the local terminal execution plane.

### Remote/control plane

MCP, tunnels, external providers, GitHub, and other remote services are downstream capabilities. They must not become hidden prerequisites for a local interactive runtime that has already passed its local provenance and runtime checks.

## Required diagnostic order

When an interactive terminal fails, investigate in this order:

1. PTY creation;
2. child PID/process liveness;
3. PRoot execution;
4. loader validation/execution;
5. rootfs access;
6. shell startup;
7. terminal output/transcript;
8. prompt detection;
9. READY state;
10. only then network capability and remote transport.

Do not attribute a local terminal failure to a provider, Wi-Fi, cellular network, DNS, VPN, or tunnel without evidence that the local execution path depends on that component.

## Verification matrix

The minimum future DUT acceptance matrix must include:

| Case | Network condition | Local terminal | Network capabilities |
|---|---|---|---|
| N0 | all network unavailable | MUST remain operational | BLOCKED/DEGRADED |
| N1 | Wi-Fi | MUST remain operational | PASS/DEGRADED |
| N2 | cellular | MUST remain operational | PASS/DEGRADED |
| N3 | VPN active | MUST remain operational | PASS/DEGRADED |
| N4 | IPv4/IPv6 transition | MUST remain operational | PASS/DEGRADED |
| N5 | network flap/provider handover | MUST remain operational | transient degradation allowed |

N0 is the falsification test for the hypothesis that provider/wireless connectivity is a prerequisite for the terminal.

## Implementation boundary

`RuntimeInstaller` is allowed to use network transport because runtime acquisition is explicitly network-dependent. That dependency must remain confined to installation/acquisition and must not leak into `RuntimeSessionManager`, `InteractiveSessionContract`, PTY creation, or local runtime startup.

The invariant does not require Alfa to implement NASA DTN or Bundle Protocol. It adopts the relevant architectural property: **loss of a communication path must degrade communication capability rather than collapse unrelated local execution.**

## Primary references

- NASA, Delay/Disruption Tolerant Networking: https://www.nasa.gov/communicating-with-missions/delay-disruption-tolerant-networking/
- NASA, Delay/Disruption Tolerant Networking Overview: https://www.nasa.gov/reference/delay-disruption-tolerant-networking-overview/
- NASA NTRS, DTN Implementer's Guide Level 6, 2026: https://ntrs.nasa.gov/citations/20260000898
- Android Developers, Reading network state: https://developer.android.com/develop/connectivity/network-ops/reading-network-state
- Android Developers, VPN service: https://developer.android.com/develop/connectivity/vpn
- AOSP, DNS Resolver: https://source.android.com/docs/core/ota/modular-system/dns-resolver
- IETF RFC 8305, Happy Eyeballs Version 2: https://www.rfc-editor.org/rfc/rfc8305.html
- IETF RFC 6146, Stateful NAT64: https://www.rfc-editor.org/rfc/rfc6146.html
- IETF RFC 6147, DNS64: https://www.rfc-editor.org/rfc/rfc6147.html
