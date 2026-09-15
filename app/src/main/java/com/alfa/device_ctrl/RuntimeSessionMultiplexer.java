package com.alfa.device_ctrl;

import com.termux.view.TerminalView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns multiple independently addressable interactive sessions without a second runtime registry. */
public final class RuntimeSessionMultiplexer {
    private final Map<String, RuntimeSessionManager> sessions = new LinkedHashMap<>();
    private String activeSessionId;

    public synchronized RuntimeSessionManager createSession(String sessionId, InteractiveSessionContract contract, RuntimeSessionManager.Listener listener) {
        requireSessionId(sessionId);
        if (contract == null) throw new IllegalArgumentException("contract is required");
        if (sessions.containsKey(sessionId)) throw new IllegalStateException("session-already-exists:" + sessionId);
        RuntimeSessionManager manager = new RuntimeSessionManager(contract, listener);
        sessions.put(sessionId, manager);
        if (activeSessionId == null) activeSessionId = sessionId;
        return manager;
    }

    synchronized void adoptSession(String sessionId, RuntimeSessionManager manager, RuntimeSessionManager.Listener listener) {
        requireSessionId(sessionId);
        if (manager == null || listener == null) throw new IllegalArgumentException("manager and listener are required");
        RuntimeSessionManager existing = sessions.get(sessionId);
        if (existing != null && existing != manager) throw new IllegalStateException("session-already-owned:" + sessionId);
        manager.rebindListener(listener);
        sessions.put(sessionId, manager);
        if (activeSessionId == null) activeSessionId = sessionId;
    }

    public synchronized RuntimeSessionManager getSession(String sessionId) { return sessions.get(sessionId); }
    public synchronized boolean containsSession(String sessionId) { return sessions.containsKey(sessionId); }
    public synchronized int size() { return sessions.size(); }
    public synchronized List<String> sessionIds() { return new ArrayList<>(sessions.keySet()); }

    public synchronized void setActiveSessionId(String sessionId) {
        requireSession(sessionId);
        activeSessionId = sessionId;
    }

    public synchronized String activeSessionId() { return activeSessionId; }
    public synchronized RuntimeSessionManager activeSession() { return activeSessionId == null ? null : sessions.get(activeSessionId); }

    public synchronized boolean startSession(String sessionId, int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        RuntimeSessionManager manager = requireSession(sessionId);
        activeSessionId = sessionId;
        return manager.start(columns, rows, cellWidthPixels, cellHeightPixels);
    }

    public synchronized void attachSession(String sessionId, TerminalView view) {
        requireSession(sessionId).attachTo(view);
    }

    public synchronized void attachActiveSession(TerminalView view) {
        RuntimeSessionManager manager = activeSession();
        if (manager == null) throw new IllegalStateException("no-active-session");
        manager.attachTo(view);
    }

    /** Explicit user stop: unlike lifecycle stopAll(), this must terminate the owned PTY. */
    public synchronized void stopSession(String sessionId) {
        RuntimeSessionManager manager = sessions.get(sessionId);
        if (manager != null) manager.finishForKeepAliveStop();
    }

    /** Explicit user removal: terminate first, then remove the manager from canonical ownership. */
    public synchronized RuntimeSessionManager removeSession(String sessionId) {
        RuntimeSessionManager manager = sessions.remove(sessionId);
        if (manager != null) manager.finishForKeepAliveStop();
        if (sessionId != null && sessionId.equals(activeSessionId)) {
            activeSessionId = sessions.isEmpty() ? null : sessions.keySet().iterator().next();
        }
        return manager;
    }

    /** Lifecycle/background operation: retain sessions according to RuntimeKeepAliveService ownership semantics. */
    public synchronized void stopAll() { for (RuntimeSessionManager manager : sessions.values()) manager.stop(); }

    public synchronized void removeAll() {
        for (RuntimeSessionManager manager : sessions.values()) manager.stop();
        sessions.clear();
        activeSessionId = null;
    }

    private RuntimeSessionManager requireSession(String sessionId) {
        requireSessionId(sessionId);
        RuntimeSessionManager manager = sessions.get(sessionId);
        if (manager == null) throw new IllegalArgumentException("unknown-session:" + sessionId);
        return manager;
    }

    private static void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty() || sessionId.indexOf('\0') >= 0) throw new IllegalArgumentException("sessionId is invalid");
    }
}
