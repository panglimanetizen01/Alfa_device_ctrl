package com.alfa.device_ctrl;

import android.content.Context;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent runtime-bound VFS bind rules. Rules are applied to newly created PRoot sessions. */
public final class RuntimeDirectoryOverrideStore {
    private static final Preferences.Key<String> OVERRIDES = PreferencesKeys.stringKey("runtime_directory_overrides_v1");
    private static volatile RuntimeDirectoryOverrideStore instance;
    private final AlfaSettingsStore settings;
    private RuntimeDirectoryOverrideStore(Context context) { settings = AlfaSettingsStore.get(context); }
    public static RuntimeDirectoryOverrideStore get(Context context) {
        RuntimeDirectoryOverrideStore current = instance;
        if (current != null) return current;
        synchronized (RuntimeDirectoryOverrideStore.class) {
            if (instance == null) instance = new RuntimeDirectoryOverrideStore(context);
            return instance;
        }
    }

    public synchronized List<RuntimeDirectoryOverride> list() {
        String raw = settings.getRuntimeDirectoryOverrides("");
        if (raw.trim().isEmpty()) return Collections.emptyList();
        List<RuntimeDirectoryOverride> result = new ArrayList<>();
        for (String line : raw.split("\\n")) {
            if (line.trim().isEmpty()) continue;
            try { result.add(RuntimeDirectoryOverride.parse(line)); }
            catch (IllegalArgumentException ignored) { }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized void add(RuntimeDirectoryOverride override) {
        if (override == null) throw new IllegalArgumentException("override-required");
        List<RuntimeDirectoryOverride> current = new ArrayList<>(list());
        for (RuntimeDirectoryOverride item : current) {
            if (item.runtimeId().equals(override.runtimeId()) && item.guestPath().equals(override.guestPath())) current.remove(item);
        }
        current.add(override);
        persist(current);
    }

    public synchronized boolean remove(String runtimeId, String guestPath) {
        List<RuntimeDirectoryOverride> current = new ArrayList<>(list());
        boolean changed = current.removeIf(item -> item.runtimeId().equals(runtimeId) && item.guestPath().equals(guestPath));
        if (changed) persist(current);
        return changed;
    }

    public synchronized List<RuntimeDirectoryOverride> forRuntime(String runtimeId) {
        List<RuntimeDirectoryOverride> result = new ArrayList<>();
        for (RuntimeDirectoryOverride item : list()) if (item.appliesTo(runtimeId)) result.add(item);
        return Collections.unmodifiableList(result);
    }

    private void persist(List<RuntimeDirectoryOverride> values) {
        StringBuilder text = new StringBuilder();
        for (RuntimeDirectoryOverride value : values) {
            if (text.length() > 0) text.append('\n');
            text.append(value.serialize());
        }
        settings.setRuntimeDirectoryOverrides(text.toString());
    }
}
