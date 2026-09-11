package com.alfa.device_ctrl;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native security diagnostic screen; every status comes from a measurable local property. */
public final class SecurityDiagnosticActivity extends Activity {
    private TextView report;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(14),dp(14),dp(14)); root.setBackgroundColor(AlfaUiTheme.CANVAS);
        TextView title=new TextView(this); title.setText("SECURITY DIAGNOSTIC"); title.setTextColor(AlfaUiTheme.TEXT); title.setTextSize(16); title.setGravity(Gravity.CENTER_VERTICAL); root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));
        report=new TextView(this); report.setTextColor(AlfaUiTheme.TEXT_MUTED); report.setTextSize(11); report.setTypeface(android.graphics.Typeface.MONOSPACE); report.setGravity(Gravity.TOP|Gravity.START); root.addView(report,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this);
        Button refresh=new Button(this); refresh.setText("REFRESH EVIDENCE"); refresh.setContentDescription("Perbarui bukti diagnostik keamanan"); refresh.setMinHeight(dp(48)); refresh.setOnClickListener(v->refresh()); actions.addView(refresh,new LinearLayout.LayoutParams(0,dp(48),1));
        Button rish=new Button(this); rish.setText("PROBE RISH: id"); rish.setContentDescription("Uji eksekusi Rish dengan perintah id"); rish.setMinHeight(dp(48)); rish.setOnClickListener(v->probeRish()); actions.addView(rish,new LinearLayout.LayoutParams(0,dp(48),1)); root.addView(actions);
        setContentView(root); refresh();
    }
    @Override protected void onResume(){super.onResume();refresh();}
    private void refresh(){report.setText(SecurityEvidenceSnapshot.collect(this).asText());}
    private void probeRish(){report.setText("RISH=RUNNING\nSYSCALL=OBSERVED:/proc/self/status");new Thread(()->{SecurityExecutionProbe.Result result=SecurityExecutionProbe.probeRish();runOnUiThread(()->report.setText(SecurityEvidenceSnapshot.collect(this).asText()+"\nRISH_EXECUTION="+result.status+"\nRISH_OUTPUT="+result.output));},"alfa-rish-probe").start();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
