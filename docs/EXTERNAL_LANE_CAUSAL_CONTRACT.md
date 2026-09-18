# External Lane Causal Contract

This document records the implementation boundary established from the Stitch reference corpus, canonical Alfa source, and primary Shizuku/Termux sources.

## Reference forensic result

`stitch_alfa_device_control_v1.0.0(3).zip` declares four separate lanes:

- `ALFA RUNTIME GUEST`: PRoot/rootfs Linux userspace.
- `TERMUX EXECUTION LANE`: auxiliary host command execution.
- `TERMUX API LANE`: Android/device APIs such as battery, sensors, SMS and clipboard.
- `SHIZUKU / RISH LANE`: privileged Android shell/binder capability.

The ZIP pages that were inspected are reference/simulation material. They contain UI/log terminology but no Android `ShizukuProvider`, `bindUserService`, `RUN_COMMAND` Intent, `PendingIntent` result receiver, or equivalent causal bridge implementation. Therefore the ZIP is evidence of intended separation, not execution proof.

## Canonical runtime boundary

The Linux guest runtime remains:

`RuntimeRegistry → RuntimeProfile → RuntimeInstaller → Gate 6/7 → RuntimeSessionManager → TerminalSession → embedded PTY → PRoot → rootfs`

External lanes are siblings. They do not determine Linux runtime readiness and do not replace the embedded PTY.

## Shizuku causal path

`Alfa Activity/bridge → ShizukuProvider → Shizuku permission → bindUserService(UserServiceArgs) → AlfaShizukuUserService → child process → PID/stdout/stderr/exit`

Implementation files:

- `ShizukuExecutionBridge.java`: binder readiness, permission, UserService binding, lifecycle and result propagation.
- `AlfaShizukuUserService.java`: separate Shizuku process boundary and child process creation.
- `IAlfaShizukuService.aidl`: process control contract.
- `IAlfaShizukuCallback.aidl`: PID/output/exit evidence contract.

The child PID is emitted by the wrapper shell before `exec`, because Android API 26-compatible `java.lang.Process` does not provide the required PID API used by this project.

`Shizuku.newProcess`/`ShizukuRemoteProcess` is intentionally not used. Shizuku documents UserService as the replacement primitive for complicated requirements and states that `newProcess` lacks TTY support.

## Rish boundary

Rish is a Shizuku-backed interactive shell interface. Its own upstream implementation launches `app_process` with the Shizuku shell loader and passes arguments to the remote shell. Rish is therefore not the same primitive as Alfa's embedded PTY and is not substituted into `RuntimeSessionManager`.

The presence of a DUT `rish` file proves only that the Rish interface is available on that device. Alfa integration is proven only by the UserService bridge above and its returned process evidence.

## Termux RUN_COMMAND causal path

`Alfa → Intent(com.termux.RUN_COMMAND) → com.termux.app.RunCommandService → Termux shell wrapper → external process → PendingIntent → TermuxResultReceiverService → stdout/stderr/exit`

Implementation files:

- `TermuxRunCommandBridge.java`: package/permission detection and documented Intent construction.
- `TermuxResultReceiverService.java`: PendingIntent result delivery.

The wrapper prints its shell PID and then `exec`s the requested command so a real process boundary can be evidenced without inventing a PID. Background mode is used because upstream Termux documents that separate stdout/stderr are available for background commands; foreground mode returns a combined PTY transcript instead.

Termux's upstream documentation requires both `com.termux.permission.RUN_COMMAND` and the `allow-external-apps=true` Termux property. Package visibility for `com.termux` is declared in the Alfa manifest.

## Termux:API boundary

`Termux:API` is represented only by `TermuxApiCapability`. It is not a shell transport and exposes no process/PTY methods.

A device API command such as `termux-battery-status` must, if used, follow:

`Alfa → Termux RUN_COMMAND → Termux command → Termux:API capability → API result`

That chain must not be rewritten as `Termux:API → shell`.

## CI contract

The transport workflow now enforces both facts simultaneously:

1. the Linux runtime still uses embedded PTY/PRoot;
2. Shizuku and Termux external bridges exist only in the external lane package and are not referenced by `RuntimeSessionManager`, `InteractiveSessionContract`, or `RuntimeInstaller`.

The workflow does not treat source presence as DUT execution proof. DUT execution remains a separate gate requiring PID, command, output and exit evidence.
