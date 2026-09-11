package com.alfa.device_ctrl;

import android.content.Context;
import androidx.datastore.guava.GuavaDataStore;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesFileSerializer;
import androidx.datastore.preferences.core.PreferencesKeys;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Single-process persistent settings store backed by Preferences DataStore and Java/Guava APIs. */
public final class AlfaSettingsStore {
    private static final String FILE_NAME = "alfa_user_preferences.preferences_pb";
    private static final Preferences.Key<String> RUNTIME_ID = PreferencesKeys.stringKey("runtime_id");
    private static final Preferences.Key<String> LOCALE = PreferencesKeys.stringKey("locale");
    private static final Preferences.Key<String> HOST_TREE_URI = PreferencesKeys.stringKey("host_tree_uri");
    private static final Preferences.Key<String> TERMINAL_FONT = PreferencesKeys.stringKey("terminal_font");
    private static final Preferences.Key<Integer> TERMINAL_FONT_SIZE = PreferencesKeys.intKey("terminal_font_size");
    private static final Preferences.Key<Boolean> OVERLAY_ENABLED = PreferencesKeys.booleanKey("overlay_enabled");
    private static final Preferences.Key<Integer> OVERLAY_OPACITY_PERCENT = PreferencesKeys.intKey("overlay_opacity_percent");
    private static final Preferences.Key<Integer> OVERLAY_WIDTH_DP = PreferencesKeys.intKey("overlay_width_dp");
    private static final Preferences.Key<Integer> OVERLAY_HEIGHT_DP = PreferencesKeys.intKey("overlay_height_dp");
    private static final Preferences.Key<String> RUNTIME_DIRECTORY_OVERRIDES = PreferencesKeys.stringKey("runtime_directory_overrides_v1");
    private static volatile AlfaSettingsStore instance;
    private final GuavaDataStore<Preferences> store;
    private final Executor io = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "alfa-settings-io"); t.setDaemon(true); return t; });
    private AlfaSettingsStore(Context context) { store = new GuavaDataStore.Builder<Preferences>(context.getApplicationContext(), FILE_NAME, PreferencesFileSerializer.INSTANCE).setExecutor(io).build(); }
    public static AlfaSettingsStore get(Context context) { AlfaSettingsStore current=instance; if(current!=null)return current; synchronized(AlfaSettingsStore.class){if(instance==null)instance=new AlfaSettingsStore(context);return instance;} }
    public String getRuntimeId(String fallback){return getString(RUNTIME_ID,fallback);} public void setRuntimeId(String value){setString(RUNTIME_ID,value);}
    public String getLocale(String fallback){return getString(LOCALE,fallback);} public void setLocale(String value){setString(LOCALE,value);}
    public String getHostTreeUri(String fallback){return getString(HOST_TREE_URI,fallback);} public void setHostTreeUri(String value){setString(HOST_TREE_URI,value);}
    public String getTerminalFont(String fallback){return getString(TERMINAL_FONT,fallback);} public void setTerminalFont(String value){setString(TERMINAL_FONT,value);}
    public int getTerminalFontSize(int fallback){return getInt(TERMINAL_FONT_SIZE,fallback);} public void setTerminalFontSize(int value){setInt(TERMINAL_FONT_SIZE,value);}
    public boolean getOverlayEnabled(boolean fallback){return getBoolean(OVERLAY_ENABLED,fallback);} public void setOverlayEnabled(boolean value){setBoolean(OVERLAY_ENABLED,value);}
    public int getOverlayOpacityPercent(int fallback){return getInt(OVERLAY_OPACITY_PERCENT,fallback);} public void setOverlayOpacityPercent(int value){setInt(OVERLAY_OPACITY_PERCENT,Math.max(10,Math.min(100,value)));}
    public int getOverlayWidthDp(int fallback){return getInt(OVERLAY_WIDTH_DP,fallback);} public void setOverlayWidthDp(int value){setInt(OVERLAY_WIDTH_DP,Math.max(280,Math.min(1200,value)));}
    public int getOverlayHeightDp(int fallback){return getInt(OVERLAY_HEIGHT_DP,fallback);} public void setOverlayHeightDp(int value){setInt(OVERLAY_HEIGHT_DP,Math.max(180,Math.min(900,value)));}
    public String getRuntimeDirectoryOverrides(String fallback){return getString(RUNTIME_DIRECTORY_OVERRIDES,fallback);} public void setRuntimeDirectoryOverrides(String value){setString(RUNTIME_DIRECTORY_OVERRIDES,value);}
    private <T>T read(Preferences.Key<T> key,T fallback){try{T value=store.getDataAsync().get().get(key);return value==null?fallback:value;}catch(InterruptedException e){Thread.currentThread().interrupt();return fallback;}catch(ExecutionException e){return fallback;}}
    private String getString(Preferences.Key<String> key,String fallback){return read(key,fallback);} private int getInt(Preferences.Key<Integer> key,int fallback){return read(key,fallback);} private boolean getBoolean(Preferences.Key<Boolean> key,boolean fallback){return read(key,fallback);}
    private <T>void update(Preferences.Key<T> key,T value){store.updateDataAsync(current->{MutablePreferences mutable=current.toMutablePreferences();mutable.set(key,value);return mutable.toPreferences();});}
    private void setString(Preferences.Key<String> key,String value){if(value!=null)update(key,value);} private void setInt(Preferences.Key<Integer> key,int value){update(key,value);} private void setBoolean(Preferences.Key<Boolean> key,boolean value){update(key,value);}
}
