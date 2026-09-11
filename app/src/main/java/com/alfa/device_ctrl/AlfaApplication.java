package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/** Copies the explicit build-time Gate 7 launch attestation into the private runtime vault. */
public final class AlfaApplication extends Application {
    private static final String ASSET="gate7-launch.properties";
    private static AlfaApplication instance;
    private final AtomicInteger startedActivities=new AtomicInteger();
    private volatile boolean activityPauseInProgress;

    @Override public void onCreate(){
        super.onCreate(); instance=this;
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
            @Override public void onActivityStarted(Activity a){startedActivities.incrementAndGet();}
            @Override public void onActivityStopped(Activity a){startedActivities.updateAndGet(v->Math.max(0,v-1));if(a instanceof MainActivity)activityPauseInProgress=false;}
            @Override public void onActivityCreated(Activity a,Bundle state){
                installWindowInsetsPolicy(a); AlfaUiTheme.apply(a);
                if(a instanceof MainActivity){View content=a.findViewById(android.R.id.content);if(content!=null)content.post(()->{AlfaUiShell.install((MainActivity)a);shieldLegacyRuntimeDashboard((MainActivity)a);});}
            }
            @Override public void onActivityResumed(Activity a){activityPauseInProgress=false;AlfaUiTheme.apply(a);rebindForegroundSession(a);}
            @Override public void onActivityPaused(Activity a){if(a instanceof MainActivity&&RuntimeKeepAliveService.owner()!=null)activityPauseInProgress=true;}
            @Override public void onActivitySaveInstanceState(Activity a,Bundle state){}
            @Override public void onActivityDestroyed(Activity a){AlfaUiShell.onActivityDestroyed(a);}
        });
        installLaunchContract();
    }

    public static AlfaApplication getInstance(){return instance;}
    public static boolean hasVisibleActivity(){return instance!=null&&instance.startedActivities.get()>0;}
    public static boolean isActivityPauseInProgress(){return instance!=null&&instance.activityPauseInProgress;}

    private static void shieldLegacyRuntimeDashboard(MainActivity a){try{Field f=MainActivity.class.getDeclaredField("runtimeDashboard");f.setAccessible(true);f.set(a,new LinearLayout(a));}catch(Exception ignored){}}

    private static void installWindowInsetsPolicy(Activity a){
        WindowCompat.enableEdgeToEdge(a.getWindow());View content=a.findViewById(android.R.id.content);if(!(content instanceof FrameLayout))return;
        final int l=content.getPaddingLeft(),t=content.getPaddingTop(),r=content.getPaddingRight(),b=content.getPaddingBottom();final int types=WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout();
        ViewCompat.setOnApplyWindowInsetsListener(content,(v,in)->{Insets i=in.getInsets(types);v.setPadding(l+i.left,t+i.top,r+i.right,b+i.bottom);return new WindowInsetsCompat.Builder(in).setInsets(types,Insets.NONE).build();});ViewCompat.requestApplyInsets(content);
    }

    private static void rebindForegroundSession(Activity a){
        if(!(a instanceof MainActivity))return;RuntimeSessionManager owner=RuntimeKeepAliveService.owner();if(owner==null||!owner.isRunning())return;
        try{Field mf=MainActivity.class.getDeclaredField("sessionManager"),tf=MainActivity.class.getDeclaredField("terminalView");mf.setAccessible(true);tf.setAccessible(true);mf.set(a,owner);owner.rebindListener((RuntimeSessionManager.Listener)a);Object v=tf.get(a);if(v instanceof com.termux.view.TerminalView)owner.attachTo((com.termux.view.TerminalView)v);}catch(ReflectiveOperationException e){throw new IllegalStateException("runtime-session-ui-rebind-failed",e);}
    }

    private void installLaunchContract(){File vault=new File(getFilesDir(),"runtime-vault");if(!vault.exists()&&!vault.mkdirs())return;File dst=new File(vault,ASSET),tmp=new File(vault,ASSET+".part");try(InputStream in=getAssets().open(ASSET);FileOutputStream out=new FileOutputStream(tmp)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);out.getFD().sync();if(!tmp.renameTo(dst)){if(dst.exists())dst.delete();if(!tmp.renameTo(dst))throw new IllegalStateException("launch-contract-publish-failed");}validate(dst);}catch(Exception ignored){tmp.delete();}}
    private static void validate(File f)throws Exception{Properties p=new Properties();try(FileInputStream in=new FileInputStream(f)){p.load(in);}String[] keys={"pipeline_run_id","runtime_id","source_commit","gate4_contract_sha256","profile_sha256","implementation_commit","runtime_registry_sha256"};for(String k:keys)require(p,k);if(RuntimeRegistry.get(p.getProperty("runtime_id"))==null)throw new IllegalStateException("unsupported-runtime-id");if(!p.getProperty("source_commit").matches("[0-9a-fA-F]{40}")||!p.getProperty("implementation_commit").matches("[0-9a-fA-F]{40}"))throw new IllegalStateException("invalid-commit");for(String k:new String[]{"gate4_contract_sha256","profile_sha256","runtime_registry_sha256"})if(!p.getProperty(k).matches("[0-9a-fA-F]{64}"))throw new IllegalStateException("invalid-hash");}
    private static void require(Properties p,String k){if(p.getProperty(k)==null||p.getProperty(k).trim().isEmpty())throw new IllegalStateException("missing-"+k);}
}
