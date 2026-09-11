package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/** Copies the explicit build-time Gate 7 launch attestation into the private runtime vault. */
public final class AlfaApplication extends Application {
    private static final String ASSET = "gate7-launch.properties";
    private static AlfaApplication instance;
    private final AtomicInteger startedActivities = new AtomicInteger();

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity activity) { startedActivities.incrementAndGet(); }
            @Override public void onActivityStopped(Activity activity) { startedActivities.updateAndGet(value -> Math.max(0, value - 1)); }
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                installWindowInsetsPolicy(activity);
                AlfaUiTheme.apply(activity);
                ExecutionLanePanel.install(activity);
            }
            @Override public void onActivityResumed(Activity activity) {
                AlfaUiTheme.apply(activity);
                ExecutionLanePanel.install(activity);
            }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
        installLaunchContract();
    }

    public static AlfaApplication getInstance() { return instance; }
    public static boolean hasVisibleActivity() { return instance != null && instance.startedActivities.get() > 0; }

    /**
     * Android 15+ lays out target-SDK-35 apps edge-to-edge. Keep the existing native Views
     * hierarchy intact, but reserve system-bar insets for interactive/content roots so top and
     * bottom controls cannot be obscured. The listener preserves the activity's original
     * content padding rather than accumulating padding across repeated inset dispatches.
     */
    private static void installWindowInsetsPolicy(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        final int baseLeft = content.getPaddingLeft();
        final int baseTop = content.getPaddingTop();
        final int baseRight = content.getPaddingRight();
        final int baseBottom = content.getPaddingBottom();
        activity.getWindow().getDecorView().setOnApplyWindowInsetsListener((decor, insets) -> {
            final int top = insets.getSystemWindowInsetTop();
            final int bottom = insets.getSystemWindowInsetBottom();
            content.setPadding(baseLeft, baseTop + top, baseRight, baseBottom + bottom);
            return insets;
        });
        activity.getWindow().getDecorView().requestApplyInsets();
    }

    private void installLaunchContract() {
        File vault = new File(getFilesDir(), "runtime-vault");
        if (!vault.exists() && !vault.mkdirs()) return;
        File destination = new File(vault, ASSET);
        File temporary = new File(vault, ASSET + ".part");
        try (InputStream input = getAssets().open(ASSET); FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
            if (!temporary.renameTo(destination)) {
                if (destination.exists()) destination.delete();
                if (!temporary.renameTo(destination)) throw new IllegalStateException("launch-contract-publish-failed");
            }
            validate(destination);
        } catch (Exception ignored) { temporary.delete(); }
    }

    private static void validate(File file) throws Exception {
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(file)) { p.load(input); }
        require(p, "pipeline_run_id"); require(p, "runtime_id"); require(p, "source_commit");
        require(p, "gate4_contract_sha256"); require(p, "profile_sha256"); require(p, "implementation_commit");
        require(p, "runtime_registry_sha256");
        if (RuntimeRegistry.get(p.getProperty("runtime_id")) == null) throw new IllegalStateException("unsupported-runtime-id");
        if (!p.getProperty("source_commit").matches("[0-9a-fA-F]{40}")) throw new IllegalStateException("invalid-source-commit");
        if (!p.getProperty("implementation_commit").matches("[0-9a-fA-F]{40}")) throw new IllegalStateException("invalid-implementation-commit");
        if (!p.getProperty("gate4_contract_sha256").matches("[0-9a-fA-F]{64}")) throw new IllegalStateException("invalid-gate4-hash");
        if (!p.getProperty("profile_sha256").matches("[0-9a-fA-F]{64}")) throw new IllegalStateException("invalid-profile-hash");
        if (!p.getProperty("runtime_registry_sha256").matches("[0-9a-fA-F]{64}")) throw new IllegalStateException("invalid-runtime-registry-hash");
    }

    private static void require(Properties p, String key) {
        if (p.getProperty(key) == null || p.getProperty(key).trim().isEmpty()) throw new IllegalStateException("missing-" + key);
    }
}
