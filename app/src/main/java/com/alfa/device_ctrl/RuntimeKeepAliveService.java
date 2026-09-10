package com.alfa.device_ctrl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** Keeps an explicitly started interactive runtime session alive while its Activity is backgrounded. */
public final class RuntimeKeepAliveService extends Service {
    private static final String CHANNEL_ID="alfa_runtime_session";
    private static final int NOTIFICATION_ID=1701;
    private static final String ACTION_STOP="com.alfa.device_ctrl.action.STOP_RUNTIME_KEEPALIVE";
    @Override public void onCreate(){super.onCreate();NotificationManager manager=getSystemService(NotificationManager.class);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,"Alfa runtime session",NotificationManager.IMPORTANCE_LOW));startForegroundCompat(buildNotification());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){if(intent!=null&&ACTION_STOP.equals(intent.getAction()))stopSelf();return START_NOT_STICKY;}
    private Notification buildNotification(){Intent stop=new Intent(this,RuntimeKeepAliveService.class).setAction(ACTION_STOP);PendingIntent stopIntent=PendingIntent.getService(this,1702,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder builder=Build.VERSION.SDK_INT>=Build.VERSION_CODES.O?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Alfa Device Ctrl").setContentText("Runtime session aktif di background").setOngoing(true).setCategory(Notification.CATEGORY_SERVICE);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);builder.addAction(new Notification.Action.Builder(null,"STOP",stopIntent).build());return builder.build();}
    private void startForegroundCompat(Notification notification){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)startForeground(NOTIFICATION_ID,notification,Build.VERSION.SDK_INT>=Build.VERSION_CODES.UPSIDE_DOWN_CAKE?ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE:ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(NOTIFICATION_ID,notification);}
    @Override public IBinder onBind(Intent intent){return null;}
}
