# Stitch v1 native implementation lock

- Reference artifact SHA-256: `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`
- Canonical reference catalog: 159 states
- UI boundary: `AlfaFinalUiPresentation`
- Operational adapter boundary: `AlfaStitchOperationalPanels`
- Runtime/session execution remains owned by `RuntimeSessionManager`
- Network evidence remains read-only and evidence-backed by `AlfaNetworkPanel`
- Split-pane and multi-session states do not fabricate a second PTY
- Floating state does not fabricate an overlay without Android overlay capability
- Host storage visibility is not inferred from app-private paths; SAF/device-layer evidence remains required
- MCP authorization/execution remains outside the presentation layer
- Geometry contract: 4dp surface radius, 48dp minimum interactive target

Completion condition remains: CI green, local Termux synchronization/build/test, fresh DUT install, and runtime/session/network verification.
