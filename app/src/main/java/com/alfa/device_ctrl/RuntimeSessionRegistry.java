package com.alfa.device_ctrl;

import android.content.Context;

import com.termux.view.TerminalView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Multi-session coordinator. Capacity is derived from the canonical RuntimeRegistry, not a UI mock. */
public final class RuntimeSessionRegistry {
    public interface Listener extends RuntimeSessionManager.Listener { }
    private static volatile RuntimeSessionRegistry instance;
    private final Context context;
    private final Map<String, RuntimeSessionManager> sessions = new LinkedHashMap<>();

    private RuntimeSessionRegistry(Context context) { this.context = context.getApplicationContext(); }

    public static RuntimeSessionRegistry get(Context context) {
        RuntimeSessionRegistry current = instance;
        if (current != null) return current;
        synchronized (RuntimeSessionRegistry.class) {
            if (instance == null) instance = new RuntimeSessionRegistry(context);
            return instance;
        }
    }

    public synchronized int capacity() { return RuntimeRegistry.all().size(); }
    public synchronized int size() { return sessions.size(); }
    public synchronized List<String> sessionIds() { return Collections.unmodifiableList(new ArrayList<>(sessions.keySet())); }
    public synchronized RuntimeSessionManager get(String runtimeId) { return sessions.get(runtimeId); }

    public synchronized RuntimeSessionManager start(String runtimeId, RuntimeSessionManager.Listener listener, int columns, int rows, int cellWidth, int cellHeight) {
        RuntimeProfile profile = RuntimeRegistry.get(runtimeId);
        if (profile == null) throw new IllegalArgumentException("unsupported-runtime-id");
        RuntimeSessionManager existing = sessions.get(runtimeId);
        if (existing != null && existing.isRunning()) return existing;
        if (sessions.size() >= capacity()) throw new IllegalStateException("session-capacity-reached:" + capacity());
        InteractiveSessionContract contract = buildContract(profile);
        RuntimeSessionManager manager = new RuntimeSessionManager(contract, listener);
        if (!manager.start(columns, rows, cellWidth, cellHeight)) throw new IllegalStateException("runtime-session-start-blocked:" + runtimeId);
        sessions.put(runtimeId, manager);
        return manager;
    }

    public synchronized void attach(String runtimeId, TerminalView view) {
        RuntimeSessionManager manager = sessions.get(runtimeId);
        if (manager == null || !manager.isRunning()) throw new IllegalStateException("session-not-running:" + runtimeId);
        manager.attachTo(view);
    }

    public synchronized void stop(String runtimeId) {
        RuntimeSessionManager manager = sessions.remove(runtimeId);
        if (manager != null) manager.stop();
    }

    public synchronized void forgetFinished(String runtimeId) {
        RuntimeSessionManager manager = sessions.get(runtimeId);
        if (manager == null || !manager.isRunning()) sessions.remove(runtimeId);
    }

    public synchronized void stopAll() {
        List<RuntimeSessionManager> current = new ArrayList<>(sessions.values());
        sessions.clear();
        for (RuntimeSessionManager manager : current) manager.stop();
    }

    private InteractiveSessionContract buildContract(RuntimeProfile profile) {
        File files = context.getFilesDir();
        File vault = new File(files, "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        File ready = new File(runtime, "READY.evidence");
        File launch = new File(vault, "gate7-launch.properties");
        if (!Gate6LaunchContract.verify(launch, profile.id())) throw new IllegalStateException("gate7-launch-not-authorized:" + profile.id());
        if (!ready.isFile()) throw new IllegalStateException("runtime-ready-evidence-missing:" + profile.id());
        File cwd = new File(files, "session-cwd-" + profile.id());
        if (!cwd.exists() && !cwd.mkdirs()) throw new IllegalStateException("session-cwd-unavailable:" + profile.id());
        return new InteractiveSessionContract(
                "session-" + profile.id(), "request-" + profile.id(), "run-" + profile.id(), profile.id(), ready,
                new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so"), new File(runtime, "rootfs"), cwd,
                new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=" + profile.promptContract() + "\\w\\$ ",
                        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                        "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()});
    }
}
