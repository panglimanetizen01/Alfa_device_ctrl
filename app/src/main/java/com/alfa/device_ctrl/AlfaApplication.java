package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
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
            @Override public void onActivityCreated(Activity activity, Bundle state) { installWindowMetricsAndInsetsPolicy(activity); AlfaUiTheme.apply(activity); AlfaFinalUiPresentation.apply(activity); }
            @Override public void onActivityResumed(Activity activity) { AlfaUiTheme.apply(activity); AlfaFinalUiPresentation.apply(activity); RuntimeStartupCoordinator.onActivityResumed(activity); }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
        installLaunchContract();
    }

    public static AlfaApplication getInstance() { return instance; }
    public static boolean hasVisibleActivity() { return instance != null && instance.startedActivities.get() > 0; }

    /** Establishes the actual Activity-window boundary and applies modern edge-to-edge insets. */
    private static void installWindowMetricsAndInsetsPolicy(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;

        ActualWindowMetrics metrics = ActualWindowMetrics.from(activity);
        content.setTag(metrics);
        AdaptivePanelLayoutController.install(content, metrics);
        content.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right != oldRight || bottom != oldBottom) {
                ActualWindowMetrics current = ActualWindowMetrics.from(activity);
                content.setTag(current);
                AdaptivePanelLayoutController.install(content, current);
            }
        });

        final int baseLeft = content.getPaddingLeft();
        final int baseTop = content.getPaddingTop();
        final int baseRight = content.getPaddingRight();
        final int baseBottom = content.getPaddingBottom();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(false);
        }
        activity.getWindow().getDecorView().setOnApplyWindowInsetsListener((decor, insets) -> {
            final int left;
            final int top;
            final int right;
            final int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets system = insets.getInsets(
                        WindowInsets.Type.systemBars()
                                | WindowInsets.Type.displayCutout()
                                | WindowInsets.Type.mandatorySystemGestures());
                left = system.left;
                top = system.top;
                right = system.right;
                bottom = system.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            content.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
            return insets;
        });
        activity.getWindow().getDecorView().requestApplyInsets();
    }

    private void installLaunchContract() {
        File vault = new File(getFilesDir(), "runtime-vault");
        if (!vault.exists() && !vault.mkdirs()) return;
        File destination = new File(vault, ASSET);
        File temporary = new File(vault, ASSET + ".part");
        if (destination.exists() && !destination.delete()) return;
        try (InputStream input = getAssets().open(ASSET); FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
            validate(temporary);
            if (!temporary.renameTo(destination)) throw new IllegalStateException("launch-contract-publish-failed");
        } catch (Exception ignored) {
            temporary.delete();
            destination.delete();
        }
    }

    private static void validate(File file) throws Exception {
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(file)) { p.load(input); }
        require(p, "schema_version"); require(p, "gate"); require(p, "gate_status");
        require(p, "launch_status"); require(p, "launch_source"); require(p, "authorization_status");
        require(p, "pipeline_run_id"); require(p, "runtime_id"); require(p, "source_commit");
        require(p, "gate4_contract_sha256"); require(p, "profile_sha256"); require(p, "implementation_commit");
        require(p, "runtime_registry_sha256");
        if (!Gate6LaunchContract.OUTPUT_SCHEMA.equals(p.getProperty("schema_version"))) throw new IllegalStateException("invalid-schema-version");
        if (!"gate7".equals(p.getProperty("gate"))) throw new IllegalStateException("invalid-gate");
        if (!"READY".equals(p.getProperty("gate_status"))) throw new IllegalStateException("invalid-gate-status");
        if (!"AUTHORIZED".equals(p.getProperty("launch_status")) || !"AUTHORIZED".equals(p.getProperty("authorization_status"))) throw new IllegalStateException("invalid-authorization-status");
        if (!"gate6-bootstrap".equals(p.getProperty("launch_source"))) throw new IllegalStateException("invalid-launch-source");
        if (RuntimeRegistry.get(p.getProperty("runtime_id")) == null) throw new IllegalStateException("unsupported-runtime-id");
        if (!RuntimeRegistry.CANONICAL_REGISTRY_SHA256.equalsIgnoreCase(p.getProperty("runtime_registry_sha256"))) throw new IllegalStateException("stale-runtime-registry");
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
