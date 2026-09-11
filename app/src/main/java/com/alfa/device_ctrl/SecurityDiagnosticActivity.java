package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native security diagnostic screen; every status comes from a measurable local property. */
public final class SecurityDiagnosticActivity extends Activity {
    private TextView report;
    @Override protected void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(14));root.setBackgroundColor(Color.rgb(19,19,21));TextView title=new TextView(this);title.setText("SECURITY DIAGNOSTIC");title.setTextColor(Color.rgb(171,199,255));title.setTextSize(16);root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));report=new TextView(this);report.setTextColor(Color.rgb(193,198,213));report.setTextSize(11);report.setTypeface(android.graphics.Typeface.MONOSPACE);root.addView(report,new LinearLayout.LayoutParams(-1,0,1));Button refresh=new Button(this);refresh.setText("REFRESH EVIDENCE");refresh.setMinHeight(dp(48));refresh.setOnClickListener(v->refresh());root.addView(refresh);setContentView(root);refresh();}
    @Override protected void onResume(){super.onResume();refresh();}
    private void refresh(){report.setText(SecurityEvidenceSnapshot.collect(this).asText());}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
