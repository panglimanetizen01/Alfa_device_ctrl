package com.alfa.device_ctrl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.view.TerminalView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Owns Android process lifetime for interactive runtime sessions and the optional real overlay terminal. */
public final class RuntimeKeepAliveService extends Service {
    private static final String CHANNEL_ID="alfa_runtime_session";
    private static final int NOTIFICATION_ID=1701;
    private static final String ACTION_START="com.alfa.device_ctrl.action.START_RUNTIME_KEEPALIVE";
    private static final String ACTION_STOP="com.alfa.device_ctrl.action.STOP_RUNTIME_KEEPALIVE";
    private static volatile boolean active;
    private static volatile RuntimeKeepAliveService instance;
    private static volatile RuntimeSessionManager floatingManager;
    private static volatile boolean floatingRequested;
    private static final Set<RuntimeSessionManager> owners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void start(Context context, RuntimeSessionManager manager) {
        if (context == null || manager == null) throw new IllegalArgumentException("context and manager are required");
        owners.add(manager);
        Intent intent = new Intent(context, RuntimeKeepAliveService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent); else context.startService(intent);
    }
    public static void stop(Context context, RuntimeSessionManager manager) {
        if (context == null) return;
        if (manager != null) owners.remove(manager);
        if (owners.isEmpty()) context.stopService(new Intent(context, RuntimeKeepAliveService.class));
    }
    public static void showFloatingOverlay(Context context, RuntimeSessionManager manager) {
        if (context == null || manager == null || !Settings.canDrawOverlays(context)) return;
        floatingManager = manager; floatingRequested = true;
        RuntimeKeepAliveService current = instance;
        if (current != null) current.main.post(() -> current.renderFloatingOverlay());
    }
    public static void hideFloatingOverlay() {
        floatingRequested = false; floatingManager = null;
        RuntimeKeepAliveService current = instance;
        if (current != null) current.main.post(current::removeFloatingOverlay);
    }
    public static boolean isActive() { return active; }
    static RuntimeSessionManager owner() { for (RuntimeSessionManager manager : owners) if (manager != null && manager.isRunning()) return manager; return null; }
    static List<RuntimeSessionManager> owners() { return new ArrayList<>(owners); }

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private LinearLayout overlayRoot;
    private WindowManager.LayoutParams overlayParams;
    private int overlayWidthDp;
    private int overlayHeightDp;
    private int overlayOpacityPercent;
    private boolean bubbleMode;

    @Override public void onCreate(){
        super.onCreate(); instance=this; windowManager=getSystemService(WindowManager.class);
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,"Alfa runtime session",NotificationManager.IMPORTANCE_LOW));
        startForegroundCompat(buildNotification()); active=true;
        if (floatingRequested) renderFloatingOverlay();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){
            List<RuntimeSessionManager> current = owners(); owners.clear(); floatingRequested=false; removeFloatingOverlay(); active=false;
            for (RuntimeSessionManager manager : current) if (manager != null) manager.finishForKeepAliveStop(); stopForegroundCompat(); stopSelf(); return START_NOT_STICKY;
        }
        active=true; if (floatingRequested) main.post(this::renderFloatingOverlay); return START_NOT_STICKY;
    }

    private void renderFloatingOverlay() {
        if (!floatingRequested || overlayRoot != null || windowManager == null || floatingManager == null || !floatingManager.isRunning() || !Settings.canDrawOverlays(this)) return;
        TerminalView terminal = new TerminalView(this, null); terminal.setTerminalViewClient(new AlfaTerminalViewClient());
        try { floatingManager.attachTo(terminal); } catch (RuntimeException error) { return; }
        AlfaSettingsStore settings = AlfaSettingsStore.get(this);
        overlayWidthDp = settings.getOverlayWidthDp(420); overlayHeightDp = settings.getOverlayHeightDp(360); overlayOpacityPercent = settings.getOverlayOpacityPercent(94);
        overlayRoot = new LinearLayout(this); overlayRoot.setOrientation(LinearLayout.VERTICAL); overlayRoot.setBackgroundColor(android.graphics.Color.rgb(19,19,21));
        TextView title = new TextView(this); title.setText("ALFA FLOATING TERMINAL"); title.setTextColor(android.graphics.Color.rgb(171,199,255)); title.setTextSize(11); title.setGravity(Gravity.CENTER_VERTICAL); title.setPadding(dp(10),0,0,0);
        Button bubble = control("BUBBLE", v -> setBubbleMode(!bubbleMode));
        Button minus = control("SIZE−", v -> resize(-40, -30));
        Button plus = control("SIZE+", v -> resize(40, 30));
        Button opacityDown = control("OPACITY−", v -> changeOpacity(-5));
        Button opacityUp = control("OPACITY+", v -> changeOpacity(5));
        Button close = control("×", v -> { RuntimeSessionManager manager=floatingManager; hideFloatingOverlay(); if(manager!=null) manager.stop(); });
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1)); header.addView(bubble,new LinearLayout.LayoutParams(dp(70),dp(48))); header.addView(minus,new LinearLayout.LayoutParams(dp(62),dp(48))); header.addView(plus,new LinearLayout.LayoutParams(dp(62),dp(48))); header.addView(opacityDown,new LinearLayout.LayoutParams(dp(88),dp(48))); header.addView(opacityUp,new LinearLayout.LayoutParams(dp(88),dp(48))); header.addView(close,new LinearLayout.LayoutParams(dp(52),dp(48)));
        overlayRoot.addView(header); overlayRoot.addView(terminal,new LinearLayout.LayoutParams(-1,0,1));
        overlayParams=new WindowManager.LayoutParams(dp(overlayWidthDp),dp(overlayHeightDp),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        overlayParams.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; overlayParams.y=dp(56); overlayParams.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE; overlayParams.alpha=overlayOpacityPercent/100f; overlayParams.setTitle("Alfa Floating Terminal");
        try { windowManager.addView(overlayRoot,overlayParams); } catch (RuntimeException error) { overlayRoot=null; overlayParams=null; }
    }
    private Button control(String label, android.view.View.OnClickListener listener){Button b=new Button(this);b.setText(label);b.setTextSize(8);b.setMinHeight(dp(48));b.setMinWidth(0);b.setPadding(0,0,0,0);b.setAllCaps(false);b.setOnClickListener(listener);return b;}
    private void resize(int dw,int dh){if(overlayParams==null||overlayRoot==null)return;overlayWidthDp=Math.max(280,Math.min(1200,overlayWidthDp+dw));overlayHeightDp=Math.max(180,Math.min(900,overlayHeightDp+dh));AlfaSettingsStore s=AlfaSettingsStore.get(this);s.setOverlayWidthDp(overlayWidthDp);s.setOverlayHeightDp(overlayHeightDp);if(bubbleMode)return;overlayParams.width=dp(overlayWidthDp);overlayParams.height=dp(overlayHeightDp);try{windowManager.updateViewLayout(overlayRoot,overlayParams);}catch(RuntimeException ignored){}}
    private void changeOpacity(int delta){if(overlayParams==null)return;overlayOpacityPercent=Math.max(10,Math.min(100,overlayOpacityPercent+delta));AlfaSettingsStore.get(this).setOverlayOpacityPercent(overlayOpacityPercent);overlayParams.alpha=overlayOpacityPercent/100f;try{windowManager.updateViewLayout(overlayRoot,overlayParams);}catch(RuntimeException ignored){}}
    private void setBubbleMode(boolean bubble){bubbleMode=bubble;if(overlayParams==null||overlayRoot==null)return;overlayParams.width=dp(bubble?64:overlayWidthDp);overlayParams.height=dp(bubble?64:overlayHeightDp);try{windowManager.updateViewLayout(overlayRoot,overlayParams);}catch(RuntimeException ignored){}}
    private void removeFloatingOverlay(){if(overlayRoot==null||windowManager==null)return;try{windowManager.removeViewImmediate(overlayRoot);}catch(RuntimeException ignored){}overlayRoot=null;overlayParams=null;bubbleMode=false;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private Notification buildNotification(){Intent stop=new Intent(this,RuntimeKeepAliveService.class).setAction(ACTION_STOP);PendingIntent stopIntent=PendingIntent.getService(this,1702,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder builder=Build.VERSION.SDK_INT>=Build.VERSION_CODES.O?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Alfa Device Ctrl").setContentText("Runtime sessions active in background").setOngoing(true).setCategory(Notification.CATEGORY_SERVICE);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);builder.addAction(new Notification.Action.Builder(null,"STOP ALL",stopIntent).build());return builder.build();}
    private void startForegroundCompat(Notification notification){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(NOTIFICATION_ID,notification);}
    private void stopForegroundCompat(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);}
    @Override public void onDestroy(){active=false;removeFloatingOverlay();instance=null;List<RuntimeSessionManager> current=owners();owners.clear();for(RuntimeSessionManager manager:current)if(manager!=null)manager.finishForKeepAliveStop();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
