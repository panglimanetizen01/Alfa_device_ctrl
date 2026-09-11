package com.alfa.device_ctrl;

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

/** Owns Android process lifetime for explicitly started concurrent interactive runtime sessions. */
public final class RuntimeKeepAliveService extends Service {
    private static final String CHANNEL_ID="alfa_runtime_session";
    private static final int NOTIFICATION_ID=1701;
    private static final String ACTION_START="com.alfa.device_ctrl.action.START_RUNTIME_KEEPALIVE";
    private static final String ACTION_STOP="com.alfa.device_ctrl.action.STOP_RUNTIME_KEEPALIVE";
    private static final RuntimeSessionRegistry REGISTRY = new RuntimeSessionRegistry();
    private static volatile boolean active;

    public static void start(Context context, RuntimeSessionManager manager) {
        if (context == null || manager == null) throw new IllegalArgumentException("context and manager are required");
        REGISTRY.add(manager);
        Intent intent = new Intent(context, RuntimeKeepAliveService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent); else context.startService(intent);
    }

    public static void stop(Context context, RuntimeSessionManager manager) {
        if (context == null) return;
        if (manager != null) REGISTRY.remove(manager);
        if (REGISTRY.isEmpty()) context.stopService(new Intent(context, RuntimeKeepAliveService.class));
    }

    public static boolean isActive() { return active || !REGISTRY.isEmpty(); }
    static RuntimeSessionManager owner() { return REGISTRY.first(); }
    static RuntimeSessionRegistry registry() { return REGISTRY; }

    @Override public void onCreate(){
        super.onCreate();
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,"Alfa runtime session",NotificationManager.IMPORTANCE_LOW));
        startForegroundCompat(buildNotification());
        active=true;
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){
            for (RuntimeSessionManager current : REGISTRY.snapshot()) current.finishForKeepAliveStop();
            REGISTRY.clear();
            active=false;
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
        builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Alfa Device Ctrl").setContentText("Runtime sessions actifs en arrière-plan").setOngoing(true).setCategory(Notification.CATEGORY_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        builder.addAction(new Notification.Action.Builder(null,"STOP ALL",stopIntent).build());
        return builder.build();
    }

    private void startForegroundCompat(Notification notification){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(NOTIFICATION_ID,notification);
    }

    private void stopForegroundCompat(){ if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true); }
    @Override public void onDestroy(){ active=false; REGISTRY.clear(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent){return null;}
}
