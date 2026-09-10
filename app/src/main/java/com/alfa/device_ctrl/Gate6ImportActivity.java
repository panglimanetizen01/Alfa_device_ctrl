package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Imports one current Gate 6 bootstrap through Android's Storage Access Framework. */
public final class Gate6ImportActivity extends Activity {
    private static final int REQUEST_OPEN = 701;
    private String expectedRuntimeId;
    private TextView evidence;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        expectedRuntimeId = RuntimeSelection.resolveId(getIntent().getStringExtra("runtime_id"));
        setTitle("Alfa Runtime Provisioning");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        TextView heading = new TextView(this);
        heading.setText("Alfa Device Ctrl — Gate 6 → Gate 7\n\n"
                + "Runtime target: " + expectedRuntimeId + "\n"
                + "Pilih bootstrap.txt dari pipeline current-run. Runtime identity diambil dari Gate 6 dan tidak dapat diganti oleh UI.");
        root.addView(heading);

        Button importButton = new Button(this);
        importButton.setText("IMPORT GATE 6 BOOTSTRAP");
        importButton.setOnClickListener(v -> openBootstrap());
        root.addView(importButton);

        evidence = new TextView(this);
        evidence.setText("GATE6_IMPORT=WAITING");
        evidence.setTextIsSelectable(true);
        root.addView(evidence, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void openBootstrap() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "application/octet-stream"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OPEN);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN || resultCode != RESULT_OK || data == null || data.getData() == null) {
            if (requestCode == REQUEST_OPEN) evidence.setText("GATE6_IMPORT=CANCELLED");
            return;
        }
        Uri uri = data.getData();
        File vault = new File(getFilesDir(), "runtime-vault");
        File incoming = new File(vault, ".gate6-bootstrap.incoming");
        File launch = new File(vault, "gate7-launch.properties");
        try {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (flags != 0) {
                try { getContentResolver().takePersistableUriPermission(uri, flags); } catch (SecurityException ignored) { }
            }
            if (!vault.exists() && !vault.mkdirs()) throw new IllegalStateException("runtime-vault cannot be created");
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(incoming)) {
                if (input == null) throw new IllegalStateException("selected document cannot be opened");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            Gate6LaunchContract.importBootstrap(incoming, launch);
            if (!Gate6LaunchContract.verify(launch, expectedRuntimeId)) throw new IllegalStateException("gate6-runtime-mismatch");
            if (!incoming.delete()) throw new IllegalStateException("incoming bootstrap cleanup failed");
            evidence.setText("GATE6_IMPORT=PASS\nGATE7_LAUNCH=READY\nRUNTIME=" + expectedRuntimeId);
            setResult(RESULT_OK);
            finish();
        } catch (Exception error) {
            launch.delete();
            incoming.delete();
            evidence.setText("GATE6_IMPORT=BLOCKED\nREASON=" + error.getClass().getSimpleName()
                    + "\nDETAIL=" + String.valueOf(error.getMessage()));
        }
    }
}
