package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** SAF-backed text editor plus an actual Android UNIX-domain socket diagnostic. */
public final class ProjectEditorSocketDiagnosticsActivity extends Activity {
    private static final int OPEN_FILE = 5201;
    private TextView state; private EditText editor; private Uri fileUri; private SafHostStorageProvider provider;
    @Override protected void onCreate(Bundle saved){super.onCreate(saved);provider=new SafHostStorageProvider(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(10),dp(10),dp(10),dp(10));TextView title=text("PROJECT EDITOR / SOCKET DIAGNOSTICS",16,true);root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));state=text("EDITOR_BACKEND=SAF\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=NOT_MEASURED",10,false);state.setTypeface(android.graphics.Typeface.MONOSPACE);root.addView(state,new LinearLayout.LayoutParams(-1,dp(70)));LinearLayout actions=new LinearLayout(this);Button choose=button("OPEN",v->chooseFile());Button save=button("SAVE",v->saveFile());Button socket=button("SOCKET SELF-TEST",v->socketSelfTest());actions.addView(choose,weight());actions.addView(save,weight());actions.addView(socket,weight());root.addView(actions,new LinearLayout.LayoutParams(-1,dp(52)));editor=new EditText(this);editor.setGravity(Gravity.TOP|Gravity.START);editor.setTextSize(11);editor.setTypeface(android.graphics.Typeface.MONOSPACE);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void chooseFile(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/*");startActivityForResult(intent,OPEN_FILE);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=OPEN_FILE||result!=RESULT_OK||data==null||data.getData()==null)return;fileUri=data.getData();try{final int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(flags!=0)getContentResolver().takePersistableUriPermission(fileUri,flags);String value=provider.readUtf8(fileUri);editor.setText(value);state.setText("EDITOR_BACKEND=SAF\nEDITOR_STATE=READ_PASS\nFILE_URI="+fileUri+"\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=NOT_MEASURED");}catch(Exception e){state.setText("EDITOR_BACKEND=SAF\nEDITOR_STATE=READ_BLOCKED\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=NOT_MEASURED");}}
    private void saveFile(){if(fileUri==null){state.setText("EDITOR_BACKEND=SAF\nEDITOR_STATE=NO_FILE_SELECTED\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=NOT_MEASURED");return;}try{provider.writeUtf8(fileUri,editor.getText().toString());state.setText("EDITOR_BACKEND=SAF\nEDITOR_STATE=WRITE_PASS\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=NOT_MEASURED");}catch(Exception e){state.setText("EDITOR_BACKEND=SAF\nEDITOR_STATE=WRITE_BLOCKED\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=NOT_MEASURED");}}
    private void socketSelfTest(){new Thread(()->{String name="alfa-socket-diagnostic-"+android.os.Process.myPid()+"-"+System.nanoTime();try(LocalServerSocket server=new LocalServerSocket(name)){Thread worker=new Thread(()->{try(LocalSocket client=new LocalSocket()){client.connect(new LocalSocketAddress(name,LocalSocketAddress.Namespace.ABSTRACT));OutputStream out=client.getOutputStream();out.write("ALFA_SOCKET_OK".getBytes(StandardCharsets.UTF_8));out.flush();}catch(Exception ignored){}});worker.start();try(LocalSocket accepted=server.accept()){InputStream in=accepted.getInputStream();byte[] buffer=new byte[64];int n=in.read(buffer);String reply=new String(buffer,0,Math.max(0,n),StandardCharsets.UTF_8);runOnUiThread(()->state.setText("EDITOR_BACKEND=SAF\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE="+("ALFA_SOCKET_OK".equals(reply)?"PASS":"FAIL")));}worker.join(3000);}catch(Exception e){runOnUiThread(()->state.setText("EDITOR_BACKEND=SAF\nSOCKET_BACKEND=LOCAL_SOCKET\nSOCKET_STATE=ERROR\nERROR="+e.getClass().getSimpleName()));}},"alfa-socket-diagnostic").start();}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return t;}private Button button(String value,android.view.View.OnClickListener listener){Button b=new Button(this);b.setText(value);b.setMinHeight(dp(48));b.setOnClickListener(listener);return b;}private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,dp(48),1);}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
