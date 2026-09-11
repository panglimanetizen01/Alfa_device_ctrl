package com.alfa.device_ctrl;

import com.termux.view.TerminalView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns the bounded set of independent RuntimeSessionManager instances presented by the UI. */
public final class RuntimeSessionRegistry {
    public static final int MAX_SESSIONS = 10;

    public interface Listener {
        void onRegistryChanged();
        void onSessionState(String sessionId, String state);
        void onSessionTextChanged(String sessionId);
        void onSessionFinished(String sessionId, int exitStatus);
    }

    public static final class Entry {
        private final String sessionId;
        private final RuntimeSessionManager manager;

        Entry(String sessionId, RuntimeSessionManager manager) {
            this.sessionId = sessionId;
            this.manager = manager;
        }

        public String sessionId() { return sessionId; }
        public RuntimeSessionManager manager() { return manager; }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private Listener listener;

    public RuntimeSessionRegistry(Listener listener) { this.listener = listener; }

    public synchronized void setListener(Listener listener) { this.listener = listener; }

    /** Reserves a unique bounded session slot before a real RuntimeSessionManager is bound to it. */
    public synchronized boolean reserve(String sessionId) {
        if (!validId(sessionId) || entries.containsKey(sessionId) || entries.size() >= MAX_SESSIONS) return false;
        entries.put(sessionId, new Entry(sessionId, null));
        notifyChanged();
        return true;
    }

    /** Binds the real RuntimeSessionManager created from the canonical InteractiveSessionContract. */
    public synchronized boolean bind(String sessionId, RuntimeSessionManager manager) {
        if (!validId(sessionId) || manager == null || !entries.containsKey(sessionId)) return false;
        if (entries.get(sessionId).manager() != null) return false;
        entries.put(sessionId, new Entry(sessionId, manager));
        notifyChanged();
        return true;
    }

    public synchronized boolean add(String sessionId, RuntimeSessionManager manager) {
        if (!validId(sessionId) || manager == null || entries.containsKey(sessionId) || entries.size() >= MAX_SESSIONS) return false;
        entries.put(sessionId, new Entry(sessionId, manager));
        notifyChanged();
        return true;
    }

    public synchronized RuntimeSessionManager get(String sessionId) {
        Entry entry = entries.get(sessionId);
        return entry == null ? null : entry.manager();
    }

    public synchronized boolean contains(String sessionId) { return entries.containsKey(sessionId); }
    public synchronized int size() { return entries.size(); }
    public synchronized int boundCount() {
        int count = 0;
        for (Entry entry : entries.values()) if (entry.manager() != null) count++;
        return count;
    }

    public synchronized List<Entry> entries() { return new ArrayList<>(entries.values()); }

    public synchronized boolean attach(String sessionId, TerminalView terminalView) {
        RuntimeSessionManager manager = get(sessionId);
        if (manager == null || terminalView == null || !manager.isRunning()) return false;
        manager.attachTo(terminalView);
        return true;
    }

    public synchronized boolean remove(String sessionId) {
        if (!entries.containsKey(sessionId)) return false;
        entries.remove(sessionId);
        notifyChanged();
        return true;
    }

    public synchronized void clear() {
        entries.clear();
        notifyChanged();
    }

    void dispatchState(String sessionId, String state) {
        Listener current;
        synchronized (this) { current = listener; }
        if (current != null) current.onSessionState(sessionId, state);
    }

    void dispatchText(String sessionId) {
        Listener current;
        synchronized (this) { current = listener; }
        if (current != null) current.onSessionTextChanged(sessionId);
    }

    void dispatchFinished(String sessionId, int exitStatus) {
        Listener current;
        synchronized (this) { entries.remove(sessionId); current = listener; }
        if (current != null) { current.onSessionFinished(sessionId, exitStatus); current.onRegistryChanged(); }
    }

    private void notifyChanged() {
        Listener current = listener;
        if (current != null) current.onRegistryChanged();
    }

    private static boolean validId(String value) { return value != null && value.matches("[A-Za-z0-9._-]{1,80}"); }
}
