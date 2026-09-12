package com.alfa.device_ctrl;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** Drives the visible first-launch path without bypassing Gate 7 or runtime evidence. */
final class RuntimeStartupCoordinator {
    private static final long RETRY_MS = 2000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Activity, Boolean> installAttempts = new WeakHashMap<>();

    private RuntimeStartupCoordinator() { }

    static void onActivityResumed(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        MAIN.post(() -> drive(activity));
    }

    private static void drive(Activity activity) {
        if (!(activity instanceof MainActivity) || activity.isFinishing() || activity.isDestroyed()) return;
        if (!AlfaApplication.hasVisibleActivity()) return;

        try {
            RuntimeProfile profile = selectedRuntime(activity);
            if (profile == null) return;

            File vault = new File(activity.getFilesDir(), "runtime-vault");
            File launch = new File(vault, "gate7-launch.properties");
            boolean gate7 = Gate6LaunchContract.verify(launch, profile.id());
            if (!gate7) return;

            File runtime = new File(new File(vault, "runtimes"), profile.id());
            File ready = new File(runtime, "READY.evidence");
            File rootfs = new File(runtime, "rootfs");
            File engine = new File(activity.getApplicationInfo().nativeLibraryDir, "libproot.so");
            boolean runtimeReady = RuntimeUiState.resolve(profile.id(), runtime, ready, engine, rootfs) == RuntimeUiState.Status.READY;
            RuntimeSessionManager manager = currentManager(activity);
            boolean running = manager != null && manager.isRunning();

            if (RuntimeStartupPolicy.shouldAutoStart(true, running, gate7, runtimeReady)) {
                invoke(activity, "startSession");
                return;
            }

            if (!runtimeReady && !installAttempts.containsKey(activity)) {
                installAttempts.put(activity, Boolean.TRUE);
                invoke(activity, "installRuntime");
            }
            MAIN.postDelayed(() -> drive(activity), RETRY_MS);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Startup must fail closed; the explicit terminal controls remain available to the user.
        }
    }

    private static RuntimeProfile selectedRuntime(Activity activity) {
        String id = activity.getPreferences(Activity.MODE_PRIVATE).getString("runtime_id", RuntimeSelection.DEFAULT_RUNTIME_ID);
        return RuntimeSelection.profile(id);
    }

    private static RuntimeSessionManager currentManager(Activity activity) throws ReflectiveOperationException {
        Field field = MainActivity.class.getDeclaredField("sessionManager");
        field.setAccessible(true);
        return (RuntimeSessionManager) field.get(activity);
    }

    private static void invoke(Activity activity, String methodName) throws ReflectiveOperationException {
        Method method = MainActivity.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(activity);
    }
}
