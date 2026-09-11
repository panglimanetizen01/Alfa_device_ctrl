package com.alfa.device_ctrl;

import android.content.Context;
import com.termux.view.TerminalView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Canonical registry: service ownership and explicit session identity share one manager instance. */
public final class RuntimeSessionRegistry {
    public interface Listener extends RuntimeSessionManager.Listener { }
    private static volatile RuntimeSessionRegistry instance;
    private final Set<RuntimeSessionManager> managers = new LinkedHashSet<>();
    private final Map<String, RuntimeSessionManager> sessions = new LinkedHashMap<>();
    private final Context context;

    public RuntimeSessionRegistry() { this.context = null; }
    private RuntimeSessionRegistry(Context context) { this.context = context.getApplicationContext(); }
    public static RuntimeSessionRegistry get(Context context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        RuntimeSessionRegistry current = instance;
        if (current != null) return current;
        synchronized (RuntimeSessionRegistry.class) { if (instance == null) instance = new RuntimeSessionRegistry(context); return instance; }
    }
    public synchronized void add(RuntimeSessionManager manager) { if (manager == null) throw new IllegalArgumentException("manager is required"); managers.add(manager); }
    public synchronized boolean remove(RuntimeSessionManager manager) { if (manager == null) return false; boolean removed = managers.remove(manager); sessions.values().removeIf(value -> value == manager); return removed; }
    public synchronized boolean isEmpty() { return managers.isEmpty(); }
    public synchronized int size() { return managers.size(); }
    public synchronized RuntimeSessionManager first() { return managers.isEmpty() ? null : managers.iterator().next(); }
    public synchronized List<RuntimeSessionManager> snapshot() { return new ArrayList<>(managers); }
    public synchronized void clear() { managers.clear(); sessions.clear(); }
    public synchronized int capacity() { return RuntimeRegistry.all().size(); }
    public synchronized List<String> sessionIds() { return Collections.unmodifiableList(new ArrayList<>(sessions.keySet())); }
    public synchronized RuntimeSessionManager get(String sessionKey) { return sessions.get(sessionKey); }
    public synchronized RuntimeSessionManager start(String runtimeId, RuntimeSessionManager.Listener listener, int columns, int rows, int cellWidth, int cellHeight) { return startSession(runtimeId, runtimeId, listener, columns, rows, cellWidth, cellHeight); }
    public synchronized RuntimeSessionManager startSession(String sessionKey, String runtimeId, RuntimeSessionManager.Listener listener, int columns, int rows, int cellWidth, int cellHeight) {
        if (context == null) throw new IllegalStateException("context-required-for-session-start");
        if (sessionKey == null || sessionKey.trim().isEmpty()) throw new IllegalArgumentException("session-key-required");
        RuntimeProfile profile = RuntimeRegistry.get(runtimeId); if (profile == null) throw new IllegalArgumentException("unsupported-runtime-id");
        RuntimeSessionManager existing = sessions.get(sessionKey); if (existing != null && existing.isRunning()) { existing.rebindListener(listener); return existing; }
        if (sessions.size() >= capacity()) throw new IllegalStateException("session-capacity-reached:" + capacity());
        InteractiveSessionContract contract = buildContract(profile, sessionKey);
        RuntimeSessionManager manager = new RuntimeSessionManager(contract, listener);
        if (!manager.start(columns, rows, cellWidth, cellHeight)) throw new IllegalStateException("runtime-session-start-blocked:" + runtimeId);
        sessions.put(sessionKey, manager); managers.add(manager); return manager;
    }
    public synchronized void attach(String sessionKey, TerminalView view) { RuntimeSessionManager manager = sessions.get(sessionKey); if (manager == null || !manager.isRunning()) throw new IllegalStateException("session-not-running:" + sessionKey); manager.attachTo(view); }
    public synchronized void stop(String sessionKey) { RuntimeSessionManager manager = sessions.remove(sessionKey); if (manager != null) { managers.remove(manager); manager.stop(); } }
    public synchronized void forgetFinished(String sessionKey) { RuntimeSessionManager manager = sessions.get(sessionKey); if (manager == null || !manager.isRunning()) { sessions.remove(sessionKey); if (manager != null) managers.remove(manager); } }
    public synchronized void stopAll() { List<RuntimeSessionManager> current = new ArrayList<>(managers); managers.clear(); sessions.clear(); for (RuntimeSessionManager manager : current) manager.stop(); }
    private InteractiveSessionContract buildContract(RuntimeProfile profile, String sessionKey) {
        File files = context.getFilesDir(); File vault = new File(files, "runtime-vault"); File runtime = new File(new File(vault, "runtimes"), profile.id()); File ready = new File(runtime, "READY.evidence"); File launch = new File(vault, "gate7-launch.properties");
        if (!Gate6LaunchContract.verify(launch, profile.id())) throw new IllegalStateException("gate7-launch-not-authorized:" + profile.id());
        if (!ready.isFile()) throw new IllegalStateException("runtime-ready-evidence-missing:" + profile.id());
        File cwd = new File(files, "session-cwd-" + profile.id() + "-" + sessionKey); if (!cwd.exists() && !cwd.mkdirs()) throw new IllegalStateException("session-cwd-unavailable:" + profile.id());
        List<RuntimeDirectoryOverride> overrides = RuntimeDirectoryOverrideStore.get(context).forRuntime(profile.id()); List<String> binds = new ArrayList<>(); File rootfs = new File(runtime, "rootfs");
        for (RuntimeDirectoryOverride override : overrides) { File target = new File(rootfs, override.guestPath().substring(1)); if (!target.exists() && !target.mkdirs()) throw new IllegalStateException("directory-override-target-unavailable:" + override.guestPath()); binds.add(override.prootBindArgument()); }
        return new InteractiveSessionContract("session-" + sessionKey, "request-" + sessionKey, "run-" + profile.id(), profile.id(), ready, new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so"), rootfs, cwd, new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=" + profile.promptContract() + "\\w\\$ ", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()}, binds.toArray(new String[0]));
    }
}
