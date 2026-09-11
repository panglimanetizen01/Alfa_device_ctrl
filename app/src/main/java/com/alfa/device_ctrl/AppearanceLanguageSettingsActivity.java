package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Locale;

/** Persistent appearance/language controls backed by Preferences DataStore. */
public final class AppearanceLanguageSettingsActivity extends Activity {
    private TextView report;
    @Override protected void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(19,19,21));root.setPadding(dp(14),dp(14),dp(14),dp(14));TextView title=new TextView(this);title.setText("APPEARANCE / LANGUAGE");title.setTextColor(Color.rgb(171,199,255));title.setTextSize(16);root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));report=new TextView(this);report.setTextColor(Color.rgb(193,198,213));report.setTypeface(android.graphics.Typeface.MONOSPACE);report.setTextSize(11);root.addView(report,new LinearLayout.LayoutParams(-1,dp(100)));Button id=new Button(this);id.setText("BAHASA INDONESIA");id.setOnClickListener(v->setLanguage("ID_ID"));root.addView(id,new LinearLayout.LayoutParams(-1,dp(52)));Button en=new Button(this);en.setText("ENGLISH");en.setOnClickListener(v->setLanguage("EN_US"));root.addView(en,new LinearLayout.LayoutParams(-1,dp(52)));LinearLayout sizes=new LinearLayout(this);Button minus=new Button(this);minus.setText("FONT -");minus.setOnClickListener(v->changeFont(-1));Button plus=new Button(this);plus.setText("FONT +");plus.setOnClickListener(v->changeFont(1));sizes.addView(minus,new LinearLayout.LayoutParams(0,dp(52),1));sizes.addView(plus,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(sizes);Button font=new Button(this);font.setText("USE MONOSPACE");font.setOnClickListener(v->{AlfaSettingsStore.get(this).setTerminalFont("monospace");refresh();});root.addView(font,new LinearLayout.LayoutParams(-1,dp(52)));TextView note=new TextView(this);note.setText("Settings are persisted in Preferences DataStore. Terminal font application is capability-detected at runtime; no unsupported API is assumed.");note.setTextColor(Color.rgb(193,198,213));note.setTextSize(10);note.setGravity(Gravity.TOP);root.addView(note,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();}
    private void setLanguage(String value){AlfaSettingsStore.get(this).setLocale(value);Locale locale=value.equals("EN_US")?Locale.US:new Locale("id","ID");Configuration c=new Configuration(getResources().getConfiguration());c.setLocale(locale);getResources().updateConfiguration(c,getResources().getDisplayMetrics());refresh();}
    private void changeFont(int delta){AlfaSettingsStore store=AlfaSettingsStore.get(this);int size=Math.max(8,Math.min(28,store.getTerminalFontSize(12)+delta));store.setTerminalFontSize(size);refresh();}
    private void refresh(){AlfaSettingsStore s=AlfaSettingsStore.get(this);report.setText("LOCALE="+s.getLocale("ID_ID")+"\nTERMINAL_FONT="+s.getTerminalFont("monospace")+"\nTERMINAL_FONT_SIZE="+s.getTerminalFontSize(12)+"\nDATASTORE=PERSISTENT");}
    static void applyToTerminal(Object terminal,int size){try{Method m=terminal.getClass().getMethod("setTextSize",int.class);m.invoke(terminal,size);}catch(Exception ignored){}}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
