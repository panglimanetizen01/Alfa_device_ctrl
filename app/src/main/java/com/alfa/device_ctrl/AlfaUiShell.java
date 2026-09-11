package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Native Views shell for UI-01..UI-05. It delegates execution to the existing Alfa runtime engine. */
public final class AlfaUiShell {
    private static final int CANVAS = Color.rgb(11, 15, 20);
    private static final int SURFACE_1 = Color.rgb(18, 24, 32);
    private static final int SURFACE_2 = Color.rgb(26, 34, 45);
    private static final int BORDER = Color.rgb(31, 41, 55);
    private static final int FOCUS = Color.rgb(55, 65, 81);
    private static final int EMERALD = Color.rgb(16, 185, 129);
    private static final int AMBER = Color.rgb(245, 158, 11);
    private static final int CRIMSON = Color.rgb(239, 68, 68);
    private static final int CYAN = Color.rgb(6, 182, 212);
    private static final int SLATE = Color.rgb(100, 116, 139);

    private static AlfaUiShell active;
    private final MainActivity activity;
    private final List<SessionSlot> slots = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout nav;
    private LinearLayout content;
    private LinearLayout dashboard;
    private LinearLayout workspace;
    private LinearLayout explorer;
    private TextView state;
    private TextView explorerBody;
    private TextView selectedPath;
    private TerminalView primaryTerminal;
    private FrameLayout terminalHost;
    private int activeSlot = -1;
    private int treeRequest = 6101;

    private AlfaUiShell(MainActivity activity) { this.activity = activity; }

    public static void install(MainActivity activity) {
        AlfaUiShell shell = new AlfaUiShell(activity);
        active = shell;
        shell.build();
    }

    public static void onActivityDestroyed(Activity activity) {
        if (active == null || active.activity != activity) return;
        active.closeAllSessions();
        active = null;
    }

    private void build() {
        WindowCompat.enableEdgeToEdge(activity.getWindow());
        root = column(CANVAS);
        root.setId(View.generateViewId());
        root.addView(topBar(), lp(-1, 56));
        root.addView(mainNavigation(), lp(-1, 52));
        content = column(CANVAS);
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        content.addView(runtimeDashboard(), lp(-1, -2));
        content.addView(space(8));
        content.addView(workspace(), new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(quickBar(), lp(-1, 56));
        activity.setContentView(root);
        bindMainActivityFields();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            android.graphics.Insets i = insets.toPlatformInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(i.left, i.top, i.right, i.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> applyAdaptive(r-l,b-t));
        applyAdaptive(root.getWidth(), root.getHeight());
    }

    private View topBar() {
        LinearLayout bar = row(SURFACE_2); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(12),0,dp(12),0);
        TextView title = label("ALFA DEVICE CTRL", EMERALD, 15, true); bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        state = label("SESSION: NOT READY", SLATE, 10, true); state.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); bar.addView(state, lp(-2, 48));
        return bar;
    }

    private View mainNavigation() {
        nav = row(SURFACE_1); nav.setPadding(dp(6),2,dp(6),2);
        addNav("RUNTIME", EMERALD, v -> showRuntime());
        addNav("TERMINAL", SLATE, v -> showTerminal());
        addNav("PROJECT", CYAN, v -> showProject());
        addNav("DIAGNOSTIC", AMBER, v -> invoke("showHtop"));
        addNav("NETWORK", CYAN, v -> invoke("showNetwork"));
        return nav;
    }

    private View runtimeDashboard() {
        dashboard = column(SURFACE_1);
        LinearLayout head = row(SURFACE_2); head.setGravity(Gravity.CENTER_VERTICAL); head.setPadding(dp(10),0,dp(8),0);
        TextView t = label("RUNTIME DASHBOARD", EMERALD, 11, true); head.addView(t, new LinearLayout.LayoutParams(0,40,1));
        Button add = button("NEW SESSION", EMERALD, v -> createSession()); add.setContentDescription("Buat sesi runtime nyata baru"); head.addView(add, lp(120,48));
        Button install = button("INSTALL", AMBER, v -> invoke("installRuntime")); install.setContentDescription("Instal runtime Linux terpilih"); head.addView(install, lp(88,48));
        dashboard.addView(head);
        refreshDashboard();
        return dashboard;
    }

    private void refreshDashboard() {
        if (dashboard == null) return;
        while (dashboard.getChildCount() > 1) dashboard.removeViewAt(1);
        for (RuntimeProfile p : RuntimeRegistry.all()) {
            File runtime = new File(new File(activity.getFilesDir(), "runtime-vault/runtimes"), p.id());
            RuntimeUiState.Status s = RuntimeUiState.resolve(p.id(), runtime, new File(runtime,"READY.evidence"), new File(activity.getApplicationInfo().nativeLibraryDir,"libproot.so"), new File(runtime,"rootfs"));
            LinearLayout card = row(SURFACE_2); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(8),0,dp(6),0);
            TextView name = label(p.displayName()+"  "+p.architecture(), Color.WHITE, 10, true); name.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); card.addView(name,new LinearLayout.LayoutParams(0,48,1));
            TextView badge = label(RuntimeUiState.label(s), s == RuntimeUiState.Status.READY ? EMERALD : (s == RuntimeUiState.Status.FAILED ? CRIMSON : SLATE), 9, true); badge.setGravity(Gravity.CENTER); card.addView(badge,lp(94,48));
            Button open = button("OPEN", s == RuntimeUiState.Status.READY ? EMERALD : SLATE, v -> { selectRuntime(p); createSession(); }); open.setEnabled(s == RuntimeUiState.Status.READY); card.addView(open,lp(68,48));
            dashboard.addView(card,lp(-1,52));
        }
    }

    private View workspace() {
        workspace = row(CANVAS);
        explorer = column(SURFACE_1); explorer.setPadding(dp(6),dp(6),dp(6),dp(6));
        workspace.addView(explorer,new LinearLayout.LayoutParams(dp(250),-1));
        workspace.addView(terminalWorkspace(),new LinearLayout.LayoutParams(0,-1,1));
        return workspace;
    }

    private View terminalWorkspace() {
        LinearLayout pane = column(SURFACE_1); pane.setBackground(borderBackground(SURFACE_1,4));
        LinearLayout head = row(SURFACE_2); head.setGravity(Gravity.CENTER_VERTICAL); head.setPadding(dp(8),0,dp(6),0);
        TextView title = label("PTY SESSION MULTIPLEXER", CYAN, 10, true); head.addView(title,new LinearLayout.LayoutParams(0,40,1));
        Button split = button("SPLIT", CYAN, v -> toggleSplit()); split.setContentDescription("Buka panel split terminal"); head.addView(split,lp(66,48));
        Button floatB = button("FLOAT", CYAN, v -> toggleFloating()); floatB.setContentDescription("Buka terminal floating dalam jendela kerja"); head.addView(floatB,lp(70,48));
        Button stop = button("STOP", CRIMSON, v -> stopActiveSession()); stop.setContentDescription("Hentikan sesi terminal aktif"); head.addView(stop,lp(64,48));
        pane.addView(head);
        LinearLayout tabs = row(SURFACE_1); pane.addView(tabs,lp(-1,50));
        terminalHost = new FrameLayout(activity); terminalHost.setBackgroundColor(Color.BLACK); pane.addView(terminalHost,new LinearLayout.LayoutParams(-1,0,1));
        addTerminalInputBar(pane);
        refreshTabs(tabs);
        return pane;
    }

    private void addTerminalInputBar(LinearLayout pane) {
        LinearLayout row = row(SURFACE_2); row.setPadding(dp(5),dp(4),dp(5),dp(4));
        String[] keys={"ESC","CTRL","ALT","TAB","←","↓","↑","→"};
        for(String key:keys){ Button b=button(key,SLATE,v -> sendKey(key)); b.setContentDescription("Kirim "+key+" ke PTY"); row.addView(b,new LinearLayout.LayoutParams(0,48,1)); }
        EditText command = new EditText(activity); command.setSingleLine(true); command.setHint("command input"); command.setTextColor(Color.WHITE); command.setHintTextColor(SLATE); command.setBackground(borderBackground(CANVAS,4)); command.setMinHeight(dp(48)); command.setOnEditorActionListener((v,a,e)->{ String text=v.getText().toString(); if(!text.isEmpty()){ sendText(text+"\n"); v.setText(""); } return true; }); row.addView(command,new LinearLayout.LayoutParams(0,48,2));
        pane.addView(row,lp(-1,56));
    }

    private View quickBar() {
        LinearLayout bar=row(SURFACE_2); bar.setPadding(dp(6),dp(4),dp(6),dp(4));
        Button files=button("PROJECT EXPLORER",CYAN,v->showProject()); files.setContentDescription("Buka project explorer"); bar.addView(files,new LinearLayout.LayoutParams(0,48,1));
        Button saf=button("SAF",CYAN,v->openTreePicker()); saf.setContentDescription("Pilih direktori melalui Storage Access Framework"); bar.addView(saf,new LinearLayout.LayoutParams(0,48,1));
        Button policy=button("POLICY",AMBER,v->invoke("showPolicy")); policy.setContentDescription("Tampilkan policy runtime"); bar.addView(policy,new LinearLayout.LayoutParams(0,48,1));
        return bar;
    }

    private void showRuntime(){ dashboard.setVisibility(View.VISIBLE); explorer.setVisibility(View.GONE); workspace.setVisibility(View.GONE); content.removeAllViews(); content.addView(dashboard); content.addView(space(8)); content.addView(runtimeWorkspace(),new LinearLayout.LayoutParams(-1,0,1)); }
    private void showTerminal(){ dashboard.setVisibility(View.VISIBLE); explorer.setVisibility(View.GONE); workspace.setVisibility(View.VISIBLE); refreshDashboard(); }
    private void showProject(){ dashboard.setVisibility(View.GONE); workspace.setVisibility(View.VISIBLE); explorer.setVisibility(View.VISIBLE); refreshExplorer(); }

    private View runtimeWorkspace(){ LinearLayout box=column(SURFACE_1); TextView t=label("REAL RUNTIME STATE\nNo mock telemetry. Open a verified runtime to attach a real PTY.\nSession states: NOT_READY / STARTING / RUNNING / FAILED / FINISHED.",SLATE,12,false); t.setPadding(dp(14),dp(14),dp(14),dp(14)); box.addView(t,lp(-1,-1)); return box; }

    private void refreshExplorer(){ explorer.removeAllViews(); TextView h=label("PROJECT EXPLORER",CYAN,11,true); explorer.addView(h,lp(-1,42)); selectedPath=label("No SAF tree selected",SLATE,9,false); selectedPath.setTypeface(Typeface.MONOSPACE); explorer.addView(selectedPath,lp(-1,44)); Button pick=button("OPEN TREE",CYAN,v->openTreePicker()); explorer.addView(pick,lp(-1,48)); explorerBody=label("Canonical host project access is shown only when Android has real access to the selected tree.\n\n/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl",SLATE,10,false); explorerBody.setTypeface(Typeface.MONOSPACE); ScrollView scroll=new ScrollView(activity); scroll.addView(explorerBody); explorer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); File canonical=new File("/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl"); if(canonical.isDirectory()) listFileTree(canonical); }

    private void listFileTree(File dir){ StringBuilder out=new StringBuilder(); File[] files=dir.listFiles(); if(files==null){explorerBody.setText("READ_BLOCKED="+dir);return;} java.util.Arrays.sort(files,Comparator.comparing(File::getName)); int limit=Math.min(files.length,200); for(int i=0;i<limit;i++){ File f=files[i]; out.append(f.isDirectory()?"[D] ":"[F] ").append(f.getName()).append(f.isDirectory()?"/":"  "+f.length()+" B").append('\n'); } if(files.length>limit) out.append("… ").append(files.length-limit).append(" more\n"); explorerBody.setText("ROOT="+dir.getAbsolutePath()+"\n\n"+out); }

    private void openTreePicker(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); activity.startActivityForResult(i,treeRequest); }

    public void onTreeResult(int resultCode, Intent data){ if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return; Uri uri=data.getData(); int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION); try{activity.getContentResolver().takePersistableUriPermission(uri,flags);}catch(SecurityException ignored){} DocumentFile tree=DocumentFile.fromTreeUri(activity,uri); if(tree==null)return; selectedPath.setText("SAF="+uri); StringBuilder out=new StringBuilder(); DocumentFile[] children=tree.listFiles(); java.util.Arrays.sort(children,Comparator.comparing(v->String.valueOf(v.getName()))); int limit=Math.min(children.length,200); for(int i=0;i<limit;i++){DocumentFile f=children[i];out.append(f.isDirectory()?"[D] ":"[F] ").append(f.getName()).append(f.isDirectory()?"/":"  "+f.length()+" B").append('\n');} explorerBody.setText("TREE="+uri+"\nREAD="+tree.canRead()+" WRITE="+tree.canWrite()+"\n\n"+out); }

    private void createSession(){ RuntimeProfile p=selectedProfile(); if(p==null){showState("SESSION FAILED: no runtime profile");return;} File runtime=new File(new File(activity.getFilesDir(),"runtime-vault/runtimes"),p.id()); File ready=new File(runtime,"READY.evidence"); File rootfs=new File(runtime,"rootfs"); File engine=new File(activity.getApplicationInfo().nativeLibraryDir,"libproot.so"); RuntimeUiState.Status rs=RuntimeUiState.resolve(p.id(),runtime,ready,engine,rootfs); if(rs!=RuntimeUiState.Status.READY){showState("SESSION FAILED: runtime state="+RuntimeUiState.label(rs));return;} if(!Gate6LaunchContract.verify(new File(new File(activity.getFilesDir(),"runtime-vault"),"gate7-launch.properties"),p.id())){showState("SESSION FAILED: Gate 6→7 launch contract not verified");return;} try{ File cwd=new File(activity.getFilesDir(),"session-cwd-"+UUID.randomUUID().toString().substring(0,8)); if(!cwd.mkdirs())throw new IllegalStateException("session-cwd-create-failed"); InteractiveSessionContract c=new InteractiveSessionContract("session-"+shortId(),"request-"+shortId(),"run-"+shortId(),p.id(),ready,engine,rootfs,cwd,new String[]{"HOME=/root","TERM=xterm-256color","PS1=alfa:"+p.id()+":\\w\\$ ","PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","PROOT_TMP_DIR="+new File(runtime,"proot_tmp").getAbsolutePath()}); SessionSlot slot=new SessionSlot(c); slots.add(slot); slot.start(); activeSlot=slots.size()-1; refreshTabs((LinearLayout)terminalHost.getParent().getParent().getChildAt(1)); showState("SESSION "+slot.contract.sessionId()+" STARTING"); }catch(Exception e){showState("SESSION FAILED: "+e.getClass().getSimpleName()+": "+e.getMessage());} }

    private RuntimeProfile selectedProfile(){ try{Field f=MainActivity.class.getDeclaredField("selectedRuntime");f.setAccessible(true);return (RuntimeProfile)f.get(activity);}catch(Exception e){return RuntimeSelection.profile(RuntimeSelection.DEFAULT_RUNTIME_ID);} }
    private void selectRuntime(RuntimeProfile p){ try{Field f=MainActivity.class.getDeclaredField("selectedRuntime");f.setAccessible(true);f.set(activity,p);activity.getPreferences(Activity.MODE_PRIVATE).edit().putString("runtime_id",p.id()).apply();}catch(Exception ignored){} refreshDashboard(); }
    private void refreshTabs(LinearLayout tabs){tabs.removeAllViews(); for(int i=0;i<slots.size();i++){final int index=i;SessionSlot s=slots.get(i);Button b=button((i+1)+":"+s.contract.runtimeId(),i==activeSlot?EMERALD:SLATE,v->{activeSlot=index;attachActive();refreshTabs(tabs);});tabs.addView(b,new LinearLayout.LayoutParams(0,48,1));}}
    private void attachActive(){ if(activeSlot<0||activeSlot>=slots.size())return; SessionSlot s=slots.get(activeSlot); terminalHost.removeAllViews(); terminalHost.addView(s.view,new FrameLayout.LayoutParams(-1,-1)); s.manager.attachTo(s.view); state.setText("SESSION: "+SessionUiState.label(s.uiState)); }
    private void stopActiveSession(){if(activeSlot<0||activeSlot>=slots.size()){showState("SESSION: NOT READY");return;}slots.get(activeSlot).manager.stop();}
    private void closeAllSessions(){for(SessionSlot s:slots)s.manager.stop();slots.clear();activeSlot=-1;}

    private void toggleSplit(){ if(activeSlot<0){showState("SPLIT BLOCKED: no real PTY session");return;} LinearLayout split=column(Color.BLACK); TerminalView second=new TerminalView(activity,null); second.setTerminalViewClient(new AlfaTerminalViewClient()); split.addView(second,new LinearLayout.LayoutParams(-1,0,1)); TextView divider=label("SPLIT VIEW — same active PTY; no second session is claimed",AMBER,9,true); divider.setGravity(Gravity.CENTER); split.addView(divider,lp(-1,28)); terminalHost.removeAllViews(); terminalHost.addView(split,new FrameLayout.LayoutParams(-1,-1)); slots.get(activeSlot).manager.attachTo(second); }
    private void toggleFloating(){ if(activeSlot<0){showState("FLOAT BLOCKED: no real PTY session");return;} FrameLayout overlay=new FrameLayout(activity); overlay.setBackground(borderBackground(SURFACE_1,4)); TextView h=label("FLOATING TERMINAL — real active PTY",CYAN,10,true); h.setPadding(dp(8),0,dp(8),0); overlay.addView(h,lp(-1,42)); TerminalView v=new TerminalView(activity,null);v.setTerminalViewClient(new AlfaTerminalViewClient());FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(-1,-1);vp.topMargin=dp(42);overlay.addView(v,vp);terminalHost.addView(overlay,new FrameLayout.LayoutParams(-1,-1));slots.get(activeSlot).manager.attachTo(v); }

    private void sendKey(String key){byte[] data;switch(key){case"ESC":data=new byte[]{27};break;case"TAB":data=new byte[]{9};break;case"←":data=new byte[]{27,'[','D'};break;case"↓":data=new byte[]{27,'[','B'};break;case"↑":data=new byte[]{27,'[','A'};break;case"→":data=new byte[]{27,'[','C'};break;default:return;}sendBytes(data);}
    private void sendText(String text){sendBytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private void sendBytes(byte[] data){if(activeSlot<0||activeSlot>=slots.size())return;TerminalSession s=slots.get(activeSlot).manager.currentSession();if(s!=null&&s.isRunning())s.write(data,0,data.length);}

    private void showState(String text){if(state!=null)state.setText(text);}
    private void invoke(String name){try{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception e){showState("ACTION FAILED="+name+" "+e.getClass().getSimpleName());}}

    private void applyAdaptive(int widthPx,int heightPx){ if(workspace==null)return; WindowManager wm=(WindowManager)activity.getSystemService(Activity.WINDOW_SERVICE); android.view.WindowMetrics m=wm.getCurrentWindowMetrics(); float d=m.getDensity(); int w=Math.round(m.getBounds().width()/d); boolean two=AdaptiveWindowPolicy.useTwoPane(w); explorer.setVisibility(two?View.VISIBLE:View.GONE); LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)explorer.getLayoutParams();p.width=dp(AdaptiveWindowPolicy.navigationWidthDp(w)==280?280:250);explorer.setLayoutParams(p); }

    private final class SessionSlot {
        final InteractiveSessionContract contract; final RuntimeSessionManager manager; final TerminalView view; SessionUiState.Status uiState=SessionUiState.Status.NOT_READY;
        SessionSlot(InteractiveSessionContract contract){this.contract=contract;this.view=new TerminalView(activity,null);this.view.setTerminalViewClient(new AlfaTerminalViewClient());this.manager=new RuntimeSessionManager(contract,new RuntimeSessionManager.Listener(){public void onState(String s){uiState=SessionUiState.resolve(s);activity.runOnUiThread(()->{if(slots.indexOf(SessionSlot.this)==activeSlot)state.setText("SESSION: "+SessionUiState.label(uiState));});}public void onTextChanged(){activity.runOnUiThread(view::invalidate);}public void onSessionFinished(int code){uiState=SessionUiState.Status.FINISHED;activity.runOnUiThread(()->{if(slots.indexOf(SessionSlot.this)==activeSlot)state.setText("SESSION: FINISHED / "+code);});}});}
        void start(){if(manager.start(80,24,8,16)){manager.attachTo(view);}}
    }

    private static String shortId(){return UUID.randomUUID().toString().replace("-","").substring(0,12);}
    private Button button(String text,int color,View.OnClickListener click){Button b=new Button(activity);b.setText(text);b.setTextColor(color);b.setTextSize(10);b.setAllCaps(false);b.setMinHeight(dp(48));b.setMinWidth(dp(48));b.setPadding(dp(5),0,dp(5),0);b.setBackground(borderBackground(SURFACE_2,4));b.setStateListAnimator(null);b.setOnClickListener(click);return b;}
    private void addNav(String text,int color,View.OnClickListener click){Button b=button(text,color,click);b.setContentDescription("Navigasi "+text);nav.addView(b,new LinearLayout.LayoutParams(0,48,1));}
    private TextView label(String text,int color,int size,boolean bold){TextView t=new TextView(activity);t.setText(text);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);return t;}
    private LinearLayout row(int color){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.HORIZONTAL);l.setBackgroundColor(color);return l;}
    private LinearLayout column(int color){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(color);return l;}
    private GradientDrawable borderBackground(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),BORDER);return d;}
    private View space(int h){return new View(activity){ {setLayoutParams(lp(-1,h));} };}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private int dp(int v){return Math.round(v*activity.getResources().getDisplayMetrics().density);}
    private void bindMainActivityFields(){setField("terminalView",primaryTerminal);setField("status",state);setField("runtimeDashboard",dashboard);setField("monitorText",state);setField("terminalTitle",state);setField("activeTab",state);setField("telemetryText",state);setField("runtimeSummary",state);setField("uiActive",true);}
    private void setField(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);if(f.getType()==boolean.class)f.setBoolean(activity,(Boolean)value);else f.set(activity,value);}catch(Exception ignored){}}
}
