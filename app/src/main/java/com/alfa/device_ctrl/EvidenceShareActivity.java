package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Arrays;

/** Secure evidence export through content:// URI grants; raw private paths are never shared. */
public final class EvidenceShareActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.setBackgroundColor(android.graphics.Color.rgb(19,19,21));
        TextView title = new TextView(this); title.setText("EVIDENCE SHARING"); title.setTextColor(android.graphics.Color.rgb(171,199,255)); title.setTextSize(16); root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));
        File g17 = new File(getFilesDir(), "g17");
        File[] files = g17.isDirectory() ? g17.listFiles(File::isFile) : new File[0];
        if (files.length == 0) {
            TextView empty = new TextView(this); empty.setText("EVIDENCE=NOT_AVAILABLE\nNo real G17 evidence file exists yet."); empty.setTextColor(android.graphics.Color.rgb(193,198,213)); empty.setTypeface(android.graphics.Typeface.MONOSPACE); root.addView(empty); 
        } else {
            Arrays.sort(files, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File file : files) {
                Button share = new Button(this); share.setText("SHARE  " + file.getName()); share.setMinHeight(dp(52)); share.setContentDescription("Bagikan evidence " + file.getName()); share.setOnClickListener(v -> shareFile(file)); root.addView(share,new LinearLayout.LayoutParams(-1,dp(56)));
            }
        }
        setContentView(root);
    }

    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName()+".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Share Alfa evidence"));
        } catch (IllegalArgumentException error) {
            TextView message = new TextView(this); message.setText("SHARE=BLOCKED\n"+error.getMessage()); message.setTextColor(android.graphics.Color.rgb(255,180,171)); setContentView(message);
        }
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
