package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Owns Android process lifetime for one or more explicitly started interactive runtime sessions. */
public final class RuntimeKeepAliveService extends Service {
    private static final String CHANNEL_ID="alfa_runtime_session";
    private static final int NOTIFICATION_ID=1701;
    private static final String ACTION_START="com.alfa.device_ctrl.action.START_RUNTIME_KEEPALIVE";
    private static final String ACTION_STOP="com.alfa.device_ctrl.action.STOP_RUNTIME_KEEPALIVE";
    private static volatile boolean active;
    private static final Set<RuntimeSessionManager> owners = new LinkedHashSet<>();
    private static volatile boolean activityPauseInProgress;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;

    public static void start(Context context, RuntimeSessionManager manager) {
        if (context == null || manager == null) throw new IllegalArgumentException("context and manager are required");
        synchronized (owners) { owners.add(manager); }
        Intent intent = new Intent(context, RuntimeKeepAliveService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent); else context.startService(intent);
    }

    public static void stop(Context context, RuntimeSessionManager manager) {
        if (context == null) return;
        boolean empty;
        synchronized (owners) {
            if (manager != null) owners.remove(manager);
            empty = owners.isEmpty();
        }
        if (empty) context.stopService(new Intent(context, RuntimeKeepAliveService.class));
    }

    public static boolean isActive() { return active; }
    static boolean owns(RuntimeSessionManager manager) { synchronized (owners) { return manager != null && owners.contains(manager); } }
    static List<RuntimeSessionManager> ownersSnapshot() { synchronized (owners) { return new ArrayList<>(owners); } }
    static boolean isActivityPauseInProgress() { return activityPauseInProgress; }

    @Override public void onCreate(){
        super.onCreate();
        registerLifecycleBridge();
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,"Alfa runtime session",NotificationManager.IMPORTANCE_LOW));
        startForegroundCompat(buildNotification());
        active=true;
    }

    private void registerLifecycleBridge() {
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { if (activity instanceof MainActivity && ownersSnapshot().isEmpty()) activityPauseInProgress = false; }
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) { }
            @Override public void onActivityResumed(Activity activity) {
                if (!(activity instanceof MainActivity)) return;
                activityPauseInProgress = false;
                List<RuntimeSessionManager> current = ownersSnapshot();
                if (current.size() == 1 && current.get(0).isRunning()) RuntimeSessionReattachment.attach(activity, current.get(0));
            }
            @Override public void onActivityPaused(Activity activity) { if (activity instanceof MainActivity && !ownersSnapshot().isEmpty()) activityPauseInProgress = true; }
            @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        };
        AlfaApplication.getInstance().registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){
            List<RuntimeSessionManager> current = ownersSnapshot();
            synchronized (owners) { owners.clear(); }
            active=false;
            for (RuntimeSessionManager manager : current) manager.finishForKeepAliveStop();
            stopForegroundCompat();
            stopSelf();
            return START_NOT_STICKY;
        }
        active=true;
        return START_NOT_STICKY;
    }

    private Notification buildNotification(){
        Intent stop=new Intent(this,RuntimeKeepAliveService.class).setAction(ACTION_STOP);
        PendingIntent stopIntent=PendingIntent.getService(this,1702,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=Build.VERSION_CODES.O?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Alfa Device Ctrl").setContentText("Runtime sessions actifs en background").setOngoing(true).setCategory(Notification.CATEGORY_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        builder.addAction(new Notification.Action.Builder(null,"STOP",stopIntent).build());
        return builder.build();
    }

    private void startForegroundCompat(Notification notification){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(NOTIFICATION_ID,notification);
    }

    private void stopForegroundCompat(){ if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true); }
    @Override public void onDestroy(){
        if (lifecycleCallbacks != null && AlfaApplication.getInstance() != null) AlfaApplication.getInstance().unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        activityPauseInProgress=false; active=false;
        synchronized (owners) { owners.clear(); }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
