package com.alfa.device_ctrl;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Evidence-only security snapshot. Unknown is preserved when the app cannot measure a property. */
public final class SecurityEvidenceSnapshot {
    public final String packageName, uid, selinux, seccomp, shizukuInstalled, overlay, notifications, fgs, runtimeReady;
    private SecurityEvidenceSnapshot(String packageName, String uid, String selinux, String seccomp,
            String shizukuInstalled, String overlay, String notifications, String fgs, String runtimeReady) {
        this.packageName = packageName;
        this.uid = uid;
        this.selinux = selinux;
        this.seccomp = seccomp;
        this.shizukuInstalled = shizukuInstalled;
        this.overlay = overlay;
        this.notifications = notifications;
        this.fgs = fgs;
        this.runtimeReady = runtimeReady;
    }

    public static SecurityEvidenceSnapshot collect(Context context) {
        String selinux = readFirstLine("/proc/self/attr/current");
        String seccomp = readStatusValue("Seccomp");
        String shizuku = "UNKNOWN";
        try {
            context.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            shizuku = "INSTALLED";
        } catch (PackageManager.NameNotFoundException ignored) {
            shizuku = "NOT_INSTALLED";
        }
        String notifications = Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
                ? "GRANTED" : "DENIED";
        String runtimeId = AlfaSettingsStore.get(context).getRuntimeId(RuntimeSelection.DEFAULT_RUNTIME_ID);
        RuntimeProfile profile = RuntimeSelection.profile(runtimeId);
        String ready = "BLOCKED";
        if (profile != null) {
            File runtime = new File(new File(new File(context.getFilesDir(), "runtime-vault"), "runtimes"), runtimeId);
            File engine = new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so");
            ready = RuntimeEvidence.verify(new File(runtime, "READY.evidence"), runtimeId, engine,
                    new File(runtime, "rootfs")) ? "PASS" : "BLOCKED";
        }
        return new SecurityEvidenceSnapshot(context.getPackageName(), String.valueOf(android.os.Process.myUid()),
                selinux.isEmpty() ? "UNKNOWN" : selinux,
                seccomp.isEmpty() ? "UNKNOWN" : seccomp,
                shizuku,
                Settings.canDrawOverlays(context) ? "GRANTED" : "DENIED",
                notifications,
                RuntimeKeepAliveService.isActive() ? "ACTIVE" : "INACTIVE",
                ready);
    }

    public String asText() {
        StringBuilder b = new StringBuilder();
        b.append("PACKAGE=").append(packageName).append('\n');
        b.append("UID=").append(uid).append('\n');
        b.append("SELINUX_CONTEXT=").append(selinux).append('\n');
        b.append("SECCOMP_MODE=").append(seccomp).append('\n');
        b.append("SHIZUKU_PACKAGE=").append(shizukuInstalled).append(" (service state not inferred)\n");
        b.append("OVERLAY_PERMISSION=").append(overlay).append('\n');
        b.append("POST_NOTIFICATIONS=").append(notifications).append('\n');
        b.append("FGS_RUNTIME_SERVICE=").append(fgs).append('\n');
        b.append("RUNTIME_READY_EVIDENCE=").append(runtimeReady).append('\n');
        return b.toString();
    }

    private static String readFirstLine(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String s = r.readLine();
            return s == null ? "" : s.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String readStatusValue(String key) {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/self/status"))) {
            String s;
            while ((s = r.readLine()) != null) {
                if (s.startsWith(key + ":")) return s.substring(key.length() + 1).trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
