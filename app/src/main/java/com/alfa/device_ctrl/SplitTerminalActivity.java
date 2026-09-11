package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.termux.view.TerminalView;

/** Two independent PTYs with coordinated lifecycle/status, backed by the process-scoped RuntimeSessionRegistry. */
public final class SplitTerminalActivity extends Activity implements RuntimeSessionManager.Listener {
    private static final int BG=Color.rgb(19,19,21),PANEL=Color.rgb(32,31,33),PRIMARY=Color.rgb(171,199,255),MUTED=Color.rgb(193,198,213);
    private RuntimeSessionRegistry registry; private Pane left; private Pane right; private TextView status;
    @Override protected void onCreate(Bundle state){super.onCreate(state);registry=RuntimeSessionRegistry.get(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(8),dp(8),dp(8),dp(8));TextView title=text("SPLIT TERMINAL / DUAL LIVE PTY",PRIMARY,15,true);root.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));status=text("",MUTED,10,false);status.setTypeface(android.graphics.Typeface.MONOSPACE);root.addView(status,new LinearLayout.LayoutParams(-1,dp(50)));LinearLayout panes=new LinearLayout(this);panes.setOrientation(LinearLayout.HORIZONTAL);panes.setWeightSum(2f);left=pane("LEFT","split-left");right=pane("RIGHT","split-right");panes.addView(left.root,new LinearLayout.LayoutParams(0,0,1));panes.addView(right.root,new LinearLayout.LayoutParams(0,0,1));root.addView(panes,new LinearLayout.LayoutParams(-1,0,1));LinearLayout controls=new LinearLayout(this);Button stop=button("STOP ALL",MUTED,v->{registry.stopAll();left.clear();right.clear();renderStatus();});controls.addView(stop,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(controls);setContentView(root);renderStatus();renderRuntimeChoices(left);renderRuntimeChoices(right);}
    private Pane pane(String name,String key){Pane p=new Pane();p.sessionKey=key;p.root=new LinearLayout(this);p.root.setOrientation(LinearLayout.VERTICAL);p.root.setBackgroundColor(PANEL);p.root.setPadding(dp(4),dp(4),dp(4),dp(4));p.header=text(name+" / EMPTY",MUTED,10,true);p.root.addView(p.header,new LinearLayout.LayoutParams(-1,dp(34)));p.terminal=new TerminalView(this,null);p.terminal.setTerminalViewClient(new AlfaTerminalViewClient());p.root.addView(p.terminal,new LinearLayout.LayoutParams(-1,0,1));p.actions=new LinearLayout(this);p.actions.setGravity(Gravity.CENTER_VERTICAL);p.root.addView(p.actions,new LinearLayout.LayoutParams(-1,dp(50)));return p;}
    private void renderRuntimeChoices(Pane pane){pane.actions.removeAllViews();for(RuntimeProfile profile:RuntimeRegistry.all()){Button b=button(profile.id(),PRIMARY,v->open(pane,profile.id()));b.setContentDescription("Buka runtime "+profile.displayName());pane.actions.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}}
    private void open(Pane pane,String runtimeId){try{if(pane.manager!=null&&pane.manager.isRunning())registry.stop(pane.sessionKey);RuntimeSessionManager manager=registry.startSession(pane.sessionKey,runtimeId,this,60,18,8,16);registry.attach(pane.sessionKey,pane.terminal);pane.runtimeId=runtimeId;pane.manager=manager;pane.header.setText((pane.sessionKey.equals("split-left")?"LEFT":"RIGHT")+" / PTY / "+runtimeId+" / "+(manager.isPromptReady()?"READY":"WAITING"));renderStatus();}catch(Exception error){pane.header.setText((pane.sessionKey.equals("split-left")?"LEFT":"RIGHT")+" / BLOCKED");status.setText("OPEN=BLOCKED\n"+error.getClass().getSimpleName()+":"+error.getMessage());}}
    private void renderStatus(){status.setText("SESSIONS="+registry.size()+"/"+registry.capacity()+"\nACTIVE="+registry.sessionIds());}
    @Override public void onState(String state){runOnUiThread(this::renderStatus);}@Override public void onTextChanged(){runOnUiThread(()->{if(left!=null&&left.terminal!=null)left.terminal.invalidate();if(right!=null&&right.terminal!=null)right.terminal.invalidate();});}@Override public void onSessionFinished(int exitStatus){runOnUiThread(this::renderStatus);}
    private static final class Pane{LinearLayout root,actions;TextView header;TerminalView terminal;RuntimeSessionManager manager;String runtimeId,sessionKey;void clear(){runtimeId=null;manager=null;header.setText((sessionKey.equals("split-left")?"LEFT":"RIGHT")+" / EMPTY");}}
    private TextView text(String v,int c,int s,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(c);t.setTextSize(s);if(bold)t.setTypeface(android.graphics.Typeface.MONOSPACE,android.graphics.Typeface.BOLD);return t;}
    private Button button(String v,int c,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(v);b.setTextColor(c);b.setTextSize(9);b.setAllCaps(false);b.setMinHeight(dp(48));b.setMinWidth(0);b.setPadding(dp(2),0,dp(2),0);b.setOnClickListener(l);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
