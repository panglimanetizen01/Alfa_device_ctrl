package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.List;

/** Real runtime-bound VFS policy surface. Directory overrides are persisted and injected into new PRoot sessions. */
public final class RuntimeVfsPolicyActivity extends Activity {
    private TextView report;
    private EditText hostPath;
    private EditText guestPath;
    private RuntimeDirectoryOverrideStore overrideStore;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        overrideStore = RuntimeDirectoryOverrideStore.get(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(19,19,21));
        root.setPadding(dp(14),dp(14),dp(14),dp(14));
        TextView title = new TextView(this); title.setText("VFS / MOUNT / DIRECTORY POLICY"); title.setTextColor(Color.rgb(171,199,255)); title.setTextSize(16); title.setGravity(Gravity.CENTER_VERTICAL); root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));
        hostPath = field("HOST DIRECTORY (Android shared storage)"); root.addView(hostPath,new LinearLayout.LayoutParams(-1,dp(52)));
        guestPath = field("GUEST MOUNT TARGET (/mnt/... or /workspace/...)"); root.addView(guestPath,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add = new Button(this); add.setText("ADD RW BIND"); add.setContentDescription("Tambahkan directory override nyata"); add.setMinHeight(dp(48)); add.setOnClickListener(v->addOverride());
        Button remove = new Button(this); remove.setText("REMOVE BIND"); remove.setContentDescription("Hapus directory override"); remove.setMinHeight(dp(48)); remove.setOnClickListener(v->removeOverride());
        actions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1)); actions.addView(remove,new LinearLayout.LayoutParams(0,dp(52),1)); root.addView(actions);
        report = new TextView(this); report.setTextColor(Color.rgb(193,198,213)); report.setTextSize(10); report.setTypeface(android.graphics.Typeface.MONOSPACE); report.setGravity(Gravity.TOP|Gravity.START); root.addView(report,new LinearLayout.LayoutParams(-1,0,1));
        Button refresh = new Button(this); refresh.setText("REFRESH RUNTIME POLICY"); refresh.setContentDescription("Perbarui kebijakan VFS runtime"); refresh.setMinHeight(dp(48)); refresh.setOnClickListener(v->refresh()); root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(48)));
        setContentView(root); refresh();
    }

    private EditText field(String hint){EditText field=new EditText(this);field.setHint(hint);field.setSingleLine(true);field.setTextSize(12);field.setTextColor(Color.WHITE);field.setHintTextColor(Color.GRAY);field.setPadding(dp(10),0,dp(10),0);return field;}

    private void addOverride(){
        try {
            String runtimeId=AlfaSettingsStore.get(this).getRuntimeId(RuntimeSelection.DEFAULT_RUNTIME_ID);
            RuntimeDirectoryOverride override=new RuntimeDirectoryOverride(runtimeId,hostPath.getText().toString(),guestPath.getText().toString());
            overrideStore.add(override); refresh();
        } catch (Exception error) { report.setText("DIRECTORY_OVERRIDE_STATUS=BLOCKED\n"+error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage())); }
    }

    private void removeOverride(){
        try { String runtimeId=AlfaSettingsStore.get(this).getRuntimeId(RuntimeSelection.DEFAULT_RUNTIME_ID); String target=RuntimeDirectoryOverride.canonicalGuestPath(guestPath.getText().toString()); overrideStore.remove(runtimeId,target); refresh(); }
        catch (Exception error) { report.setText("DIRECTORY_OVERRIDE_STATUS=BLOCKED\n"+error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage())); }
    }

    private void refresh(){
        String runtimeId=AlfaSettingsStore.get(this).getRuntimeId(RuntimeSelection.DEFAULT_RUNTIME_ID); RuntimeProfile profile=RuntimeSelection.profile(runtimeId); StringBuilder b=new StringBuilder();
        b.append("RUNTIME_ID=").append(runtimeId).append('\n'); b.append("POLICY_ID=").append(InteractiveSessionContract.POLICY_ID).append('\n'); b.append("POLICY_VERSION=").append(InteractiveSessionContract.POLICY_VERSION).append('\n'); b.append("POLICY_SCOPE=").append(InteractiveSessionContract.POLICY_SCOPE).append('\n');
        if(profile==null){b.append("RUNTIME_POLICY=BLOCKED\nunsupported-runtime-id");report.setText(b);return;}
        File runtime=new File(new File(new File(getFilesDir(),"runtime-vault"),"runtimes"),runtimeId); File ready=new File(runtime,"READY.evidence"); File proot=new File(getApplicationInfo().nativeLibraryDir,"libproot.so"); File rootfs=new File(runtime,"rootfs"); File cwd=new File(getFilesDir(),"session-cwd-"+runtimeId);
        b.append("RUNTIME_ROOT=").append(rootfs.getAbsolutePath()).append('\n'); b.append("HOST_CWD=").append(cwd.getAbsolutePath()).append('\n'); b.append("READY_EVIDENCE=").append(ready.isFile()?"PRESENT":"MISSING").append('\n'); b.append("PROOT=").append(proot.isFile()&&proot.canExecute()?"READY":"BLOCKED").append('\n');
        b.append("CANONICAL_PROOT_BINDS="); String[] args=profile.prootArguments(); for(int i=0;i<args.length;i++) if("-b".equals(args[i])&&i+1<args.length){if(b.charAt(b.length()-1)!='=')b.append(',');b.append(args[++i]);} b.append('\n');
        List<RuntimeDirectoryOverride> overrides=overrideStore.forRuntime(runtimeId); b.append("DIRECTORY_OVERRIDE_BACKEND=PROOT_BIND\n"); b.append("DIRECTORY_OVERRIDE_STATUS=").append(overrides.isEmpty()?"EMPTY":"CONFIGURED").append('\n'); b.append("DIRECTORY_OVERRIDE_COUNT=").append(overrides.size()).append('\n');
        for(RuntimeDirectoryOverride override:overrides) b.append("OVERRIDE=").append(override.hostPath()).append(":").append(override.guestPath()).append("\n");
        b.append("OVERRIDE_MODE=RW\n"); b.append("OVERRIDE_APPLY_POINT=NEW_SESSION\n"); b.append("NOTE=PRoot binds are applied when a new runtime session is created; removing a rule does not mutate an already-running PTY.\n");
        boolean authorized=false; try{InteractiveSessionContract contract=new InteractiveSessionContract("vfs-check-"+runtimeId,"vfs-request-"+runtimeId,"vfs",runtimeId,ready,proot,rootfs,cwd,new String[]{"HOME=/root","TERM=xterm-256color","PROOT_TMP_DIR="+new File(runtime,"proot_tmp").getAbsolutePath()},toBinds(overrides));authorized=contract.isAuthorizedForInteractiveRuntime();}catch(Exception ignored){}
        b.append("RUNTIME_AUTHORIZATION=").append(authorized?"PASS":"BLOCKED"); report.setText(b);
    }

    private static String[] toBinds(List<RuntimeDirectoryOverride> values){String[] result=new String[values.size()];for(int i=0;i<values.size();i++)result[i]=values.get(i).prootBindArgument();return result;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
