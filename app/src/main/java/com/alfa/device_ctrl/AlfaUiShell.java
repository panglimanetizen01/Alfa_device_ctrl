package com.alfa.device_ctrl;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Native Views shell for UI-01..UI-05. Execution remains delegated to the real Alfa runtime engine. */
public final class AlfaUiShell {
    private static final int BG=Color.rgb(11,15,20), S1=Color.rgb(18,24,32), S2=Color.rgb(26,34,45), BORDER=Color.rgb(31,41,55);
    private static final int EMERALD=Color.rgb(16,185,129), AMBER=Color.rgb(245,158,11), CRIMSON=Color.rgb(239,68,68), CYAN=Color.rgb(6,182,212), SLATE=Color.rgb(100,116,139);
    private static AlfaUiShell active;
    private final MainActivity a;
    private final List<Slot> slots=new ArrayList<>();
    private LinearLayout content,dashboard,workspace,explorer,tabs;
    private FrameLayout terminalHost;
    private TextView sessionState,explorerText;
    private int activeSlot=-1;

    private AlfaUiShell(MainActivity a){this.a=a;}
    public static void install(MainActivity a){if(active!=null)active.closeAll();active=new AlfaUiShell(a);active.build();}
    public static void onActivityDestroyed(android.app.Activity a){if(active!=null&&active.a==a){active.closeAll();active=null;}}

    private void build(){
        WindowCompat.enableEdgeToEdge(a.getWindow());
        LinearLayout root=col(BG); root.addView(top(),lp(-1,56)); root.addView(nav(),lp(-1,52));
        content=col(BG); content.setPadding(dp(12),dp(8),dp(12),dp(8)); content.addView(runtimePanel(),lp(-1,-2)); content.addView(space(8)); content.addView(workspace(),new LinearLayout.LayoutParams(-1,0,1));
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1)); root.addView(bottom(),lp(-1,56)); a.setContentView(root); bindFields();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,in)->{Insets i=in.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return in;});
        ViewCompat.requestApplyInsets(root); root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->adaptive()); adaptive();
    }

    private View top(){LinearLayout l=row(S2);l.setGravity(Gravity.CENTER_VERTICAL);l.setPadding(dp(12),0,dp(12),0);TextView t=label("ALFA DEVICE CTRL",EMERALD,15,true);l.addView(t,new LinearLayout.LayoutParams(0,-1,1));sessionState=label("SESSION: NOT READY",SLATE,10,true);sessionState.setGravity(Gravity.CENTER);l.addView(sessionState,lp(180,48));return l;}
    private View nav(){LinearLayout l=row(S1);String[] n={"RUNTIME","TERMINAL","PROJECT","DIAGNOSTIC","NETWORK"};int[] c={EMERALD,SLATE,CYAN,AMBER,CYAN};for(int i=0;i<n.length;i++){final int x=i;Button b=button(n[i],c[i],v->{if(x==0)showRuntime();else if(x==1)showTerminal();else if(x==2)showProject();else if(x==3)invoke("showHtop");else invoke("showNetwork");});l.addView(b,new LinearLayout.LayoutParams(0,48,1));}return l;}

    private View runtimePanel(){dashboard=col(S1);LinearLayout h=row(S2);h.setGravity(Gravity.CENTER_VERTICAL);TextView t=label("RUNTIME DASHBOARD",EMERALD,11,true);h.addView(t,new LinearLayout.LayoutParams(0,40,1));Button ns=button("NEW SESSION",EMERALD,v->newSession());h.addView(ns,lp(112,48));Button ins=button("INSTALL",AMBER,v->invoke("installRuntime"));h.addView(ins,lp(82,48));dashboard.addView(h);refreshDashboard();return dashboard;}
    private void refreshDashboard(){if(dashboard==null)return;while(dashboard.getChildCount()>1)dashboard.removeViewAt(1);for(RuntimeProfile p:RuntimeRegistry.all()){File r=new File(new File(a.getFilesDir(),"runtime-vault/runtimes"),p.id());RuntimeUiState.Status s=RuntimeUiState.resolve(p.id(),r,new File(r,"READY.evidence"),new File(a.getApplicationInfo().nativeLibraryDir,"libproot.so"),new File(r,"rootfs"));LinearLayout c=row(S2);c.setGravity(Gravity.CENTER_VERTICAL);TextView n=label(p.displayName()+" / "+p.architecture(),Color.WHITE,10,true);c.addView(n,new LinearLayout.LayoutParams(0,48,1));TextView q=label(RuntimeUiState.label(s),s==RuntimeUiState.Status.READY?EMERALD:(s==RuntimeUiState.Status.FAILED?CRIMSON:SLATE),9,true);q.setGravity(Gravity.CENTER);c.addView(q,lp(94,48));Button o=button("OPEN",s==RuntimeUiState.Status.READY?EMERALD:SLATE,v->{select(p);newSession();});o.setEnabled(s==RuntimeUiState.Status.READY);c.addView(o,lp(68,48));dashboard.addView(c,lp(-1,52));}}

    private View workspace(){workspace=row(BG);explorer=col(S1);explorer.setPadding(dp(6),dp(6),dp(6),dp(6));workspace.addView(explorer,new LinearLayout.LayoutParams(dp(260),-1));workspace.addView(terminalPane(),new LinearLayout.LayoutParams(0,-1,1));refreshExplorer();return workspace;}
    private View terminalPane(){LinearLayout p=col(S1);LinearLayout h=row(S2);h.setGravity(Gravity.CENTER_VERTICAL);TextView t=label("PTY SESSION MULTIPLEXER",CYAN,10,true);h.addView(t,new LinearLayout.LayoutParams(0,40,1));Button sp=button("SPLIT",CYAN,v->split());h.addView(sp,lp(64,48));Button fl=button("FLOAT",CYAN,v->floating());h.addView(fl,lp(68,48));Button st=button("STOP",CRIMSON,v->stopActive());h.addView(st,lp(64,48));p.addView(h);tabs=row(S1);p.addView(tabs,lp(-1,50));terminalHost=new FrameLayout(a);terminalHost.setBackgroundColor(Color.BLACK);p.addView(terminalHost,new LinearLayout.LayoutParams(-1,0,1));p.addView(inputBar(),lp(-1,56));return p;}
    private View inputBar(){LinearLayout l=row(S2);String[] keys={"ESC","TAB","←","↓","↑","→"};for(String k:keys){Button b=button(k,SLATE,v->key(k));l.addView(b,new LinearLayout.LayoutParams(0,48,1));}EditText e=new EditText(a);e.setSingleLine(true);e.setHint("command");e.setTextColor(Color.WHITE);e.setHintTextColor(SLATE);e.setMinHeight(dp(48));e.setBackground(bg(BG,4));e.setOnEditorActionListener((v,x,event)->{String s=v.getText().toString();if(!s.isEmpty()){write(s+"\n");v.setText("");}return true;});l.addView(e,new LinearLayout.LayoutParams(0,48,2));return l;}

    private View bottom(){LinearLayout l=row(S2);Button p=button("PROJECT EXPLORER",CYAN,v->showProject());l.addView(p,new LinearLayout.LayoutParams(0,48,1));Button d=button("DIAGNOSTIC",AMBER,v->invoke("showHtop"));l.addView(d,new LinearLayout.LayoutParams(0,48,1));Button pol=button("POLICY",AMBER,v->invoke("showPolicy"));l.addView(pol,new LinearLayout.LayoutParams(0,48,1));return l;}
    private void showRuntime(){dashboard.setVisibility(View.VISIBLE);workspace.setVisibility(View.GONE);explorer.setVisibility(View.GONE);refreshDashboard();}
    private void showTerminal(){dashboard.setVisibility(View.VISIBLE);workspace.setVisibility(View.VISIBLE);explorer.setVisibility(View.GONE);}
    private void showProject(){dashboard.setVisibility(View.GONE);workspace.setVisibility(View.VISIBLE);explorer.setVisibility(View.VISIBLE);refreshExplorer();}

    private void refreshExplorer(){explorer.removeAllViews();explorer.addView(label("PROJECT EXPLORER",CYAN,11,true),lp(-1,42));File root=new File("/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl");explorerText=label("ROOT="+root.getAbsolutePath()+"\n",SLATE,10,false);explorerText.setTypeface(Typeface.MONOSPACE);ScrollView s=new ScrollView(a);s.addView(explorerText);explorer.addView(s,new LinearLayout.LayoutParams(-1,0,1));File[] fs=root.listFiles();if(fs==null){explorerText.append("READ_BLOCKED\n");return;}java.util.Arrays.sort(fs,Comparator.comparing(File::getName));StringBuilder b=new StringBuilder();int max=Math.min(300,fs.length);for(int i=0;i<max;i++){File f=fs[i];b.append(f.isDirectory()?"[D] ":"[F] ").append(f.getName()).append(f.isDirectory()?"/":"  "+f.length()+" B").append('\n');}if(fs.length>max)b.append("… ").append(fs.length-max).append(" more\n");explorerText.setText("ROOT="+root.getAbsolutePath()+"\n\n"+b);}

    private void newSession(){RuntimeProfile p=selected();if(p==null){state("SESSION FAILED: no runtime");return;}File r=new File(new File(a.getFilesDir(),"runtime-vault/runtimes"),p.id()),ready=new File(r,"READY.evidence"),root=new File(r,"rootfs"),engine=new File(a.getApplicationInfo().nativeLibraryDir,"libproot.so");if(RuntimeUiState.resolve(p.id(),r,ready,engine,root)!=RuntimeUiState.Status.READY){state("SESSION FAILED: runtime not READY");return;}if(!Gate6LaunchContract.verify(new File(new File(a.getFilesDir(),"runtime-vault"),"gate7-launch.properties"),p.id())){state("SESSION FAILED: Gate 6→7 not verified");return;}try{File cwd=new File(a.getFilesDir(),"session-cwd-"+shortId());if(!cwd.mkdirs())throw new IllegalStateException("cwd-create-failed");InteractiveSessionContract c=new InteractiveSessionContract("session-"+shortId(),"request-"+shortId(),"run-"+shortId(),p.id(),ready,engine,root,cwd,new String[]{"HOME=/root","TERM=xterm-256color","PS1=alfa:"+p.id()+":\\w\\$ ","PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","PROOT_TMP_DIR="+new File(r,"proot_tmp").getAbsolutePath()});Slot s=new Slot(c);slots.add(s);activeSlot=slots.size()-1;s.start();attach();refreshTabs();state("SESSION: "+SessionUiState.label(s.ui));}catch(Exception e){state("SESSION FAILED: "+e.getClass().getSimpleName()+": "+e.getMessage());}}
    private RuntimeProfile selected(){try{Field f=MainActivity.class.getDeclaredField("selectedRuntime");f.setAccessible(true);return(RuntimeProfile)f.get(a);}catch(Exception e){return RuntimeSelection.profile(RuntimeSelection.DEFAULT_RUNTIME_ID);}}
    private void select(RuntimeProfile p){try{Field f=MainActivity.class.getDeclaredField("selectedRuntime");f.setAccessible(true);f.set(a,p);a.getPreferences(0).edit().putString("runtime_id",p.id()).apply();}catch(Exception ignored){}}
    private void refreshTabs(){if(tabs==null)return;tabs.removeAllViews();for(int i=0;i<slots.size();i++){final int x=i;Slot s=slots.get(i);Button b=button((i+1)+":"+s.c.runtimeId(),x==activeSlot?EMERALD:SLATE,v->{activeSlot=x;attach();refreshTabs();});tabs.addView(b,new LinearLayout.LayoutParams(0,48,1));}}
    private void attach(){if(activeSlot<0||activeSlot>=slots.size())return;Slot s=slots.get(activeSlot);terminalHost.removeAllViews();terminalHost.addView(s.view,new FrameLayout.LayoutParams(-1,-1));s.manager.attachTo(s.view);setField("sessionManager",s.manager);setField("terminalView",s.view);}
    private void stopActive(){if(activeSlot>=0&&activeSlot<slots.size())slots.get(activeSlot).manager.stop();else state("SESSION: NOT READY");}
    private void split(){if(activeSlot<0){state("SPLIT BLOCKED: no PTY");return;}LinearLayout x=col(Color.BLACK);TerminalView v=terminal();TextView d=label("SPLIT — same real PTY, no fake second session",AMBER,9,true);d.setGravity(Gravity.CENTER);x.addView(v,new LinearLayout.LayoutParams(-1,0,1));x.addView(d,lp(-1,28));terminalHost.removeAllViews();terminalHost.addView(x,new FrameLayout.LayoutParams(-1,-1));slots.get(activeSlot).manager.attachTo(v);}
    private void floating(){if(activeSlot<0){state("FLOAT BLOCKED: no PTY");return;}FrameLayout f=new FrameLayout(a);f.setBackground(bg(S1,4));TextView h=label("FLOATING TERMINAL — real PTY",CYAN,10,true);h.setPadding(dp(8),0,0,0);f.addView(h,lp(-1,42));TerminalView v=terminal();FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-1);p.topMargin=dp(42);f.addView(v,p);terminalHost.addView(f,new FrameLayout.LayoutParams(-1,-1));slots.get(activeSlot).manager.attachTo(v);}
    private TerminalView terminal(){TerminalView v=new TerminalView(a,null);v.setTerminalViewClient(new AlfaTerminalViewClient());return v;}
    private void key(String k){byte[] b=null;switch(k){case"ESC":b=new byte[]{27};break;case"TAB":b=new byte[]{9};break;case"←":b=new byte[]{27,'[','D'};break;case"↓":b=new byte[]{27,'[','B'};break;case"↑":b=new byte[]{27,'[','A'};break;case"→":b=new byte[]{27,'[','C'};break;}if(b!=null)send(b);}
    private void write(String s){send(s.getBytes(StandardCharsets.UTF_8));}
    private void send(byte[] b){if(activeSlot<0||activeSlot>=slots.size())return;TerminalSession s=slots.get(activeSlot).manager.currentSession();if(s!=null&&s.isRunning())s.write(b,0,b.length);}
    private void state(String s){if(sessionState!=null)sessionState.setText(s);}
    private void invoke(String name){try{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}catch(Exception e){state("ACTION FAILED="+name);}}
    private void adaptive(){WindowManager wm=(WindowManager)a.getSystemService(android.content.Context.WINDOW_SERVICE);android.view.WindowMetrics m=wm.getCurrentWindowMetrics();int w=Math.round(m.getBounds().width()/m.getDensity());boolean two=AdaptiveWindowPolicy.useTwoPane(w);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)explorer.getLayoutParams();p.width=dp(AdaptiveWindowPolicy.navigationWidthDp(w));explorer.setLayoutParams(p);explorer.setVisibility(two?View.VISIBLE:View.GONE);}
    private void closeAll(){for(Slot s:slots)s.manager.stop();slots.clear();activeSlot=-1;}

    private final class Slot{final InteractiveSessionContract c;final RuntimeSessionManager manager;final TerminalView view;SessionUiState.Status ui=SessionUiState.Status.NOT_READY;Slot(InteractiveSessionContract c){this.c=c;this.view=terminal();this.manager=new RuntimeSessionManager(c,new RuntimeSessionManager.Listener(){public void onState(String s){ui=SessionUiState.resolve(s);a.runOnUiThread(()->{if(slots.indexOf(Slot.this)==activeSlot)state("SESSION: "+SessionUiState.label(ui));});}public void onTextChanged(){a.runOnUiThread(view::invalidate);}public void onSessionFinished(int code){ui=SessionUiState.Status.FINISHED;a.runOnUiThread(()->{if(slots.indexOf(Slot.this)==activeSlot)state("SESSION: FINISHED / "+code);});}});}void start(){if(manager.start(80,24,8,16))manager.attachTo(view);}}

    private void bindFields(){setField("uiActive",true);setField("status",sessionState);setField("runtimeDashboard",dashboard);setField("monitorText",sessionState);setField("terminalTitle",sessionState);setField("activeTab",sessionState);setField("telemetryText",sessionState);setField("runtimeSummary",sessionState);}
    private void setField(String n,Object v){try{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);if(f.getType()==boolean.class)f.setBoolean(a,(Boolean)v);else f.set(a,v);}catch(Exception ignored){}}
    private Button button(String t,int c,View.OnClickListener l){Button b=new Button(a);b.setText(t);b.setTextColor(c);b.setTextSize(10);b.setAllCaps(false);b.setMinHeight(dp(48));b.setMinWidth(dp(48));b.setPadding(dp(4),0,dp(4),0);b.setBackground(bg(S2,4));b.setStateListAnimator(null);b.setOnClickListener(l);return b;}
    private TextView label(String t,int c,int z,boolean bold){TextView v=new TextView(a);v.setText(t);v.setTextColor(c);v.setTextSize(z);v.setGravity(Gravity.CENTER_VERTICAL);if(bold)v.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);return v;}
    private LinearLayout row(int c){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setBackgroundColor(c);return l;}
    private LinearLayout col(int c){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(c);return l;}
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),BORDER);return d;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private View space(int h){return new View(a){ {setLayoutParams(lp(-1,h));} };}
    private int dp(int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    private String shortId(){return UUID.randomUUID().toString().replace("-","").substring(0,12);}
}
