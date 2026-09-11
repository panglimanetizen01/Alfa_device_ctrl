package com.alfa.device_ctrl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Thread-safe registry for concurrent real runtime PTY owners. */
public final class RuntimeSessionRegistry {
    private final Set<RuntimeSessionManager> managers = new LinkedHashSet<>();

    public synchronized void add(RuntimeSessionManager manager) {
        if (manager == null) throw new IllegalArgumentException("manager is required");
        managers.add(manager);
    }

    public synchronized boolean remove(RuntimeSessionManager manager) {
        return managers.remove(manager);
    }

    public synchronized boolean isEmpty() { return managers.isEmpty(); }
    public synchronized int size() { return managers.size(); }

    public synchronized RuntimeSessionManager first() {
        return managers.isEmpty() ? null : managers.iterator().next();
    }

    public synchronized List<RuntimeSessionManager> snapshot() {
        return new ArrayList<>(managers);
    }

    public synchronized void clear() { managers.clear(); }
}
