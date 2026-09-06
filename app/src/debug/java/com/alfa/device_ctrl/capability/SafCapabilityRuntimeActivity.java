package com.alfa.device_ctrl.capability;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SafCapabilityRuntimeActivity extends Activity {
    private static final int REQUEST_TREE = 7101;
    private static final String MIME_TYPE = "text/plain";
    private static final String DISPLAY_NAME = "alfa-saf-runtime-probe.txt";

    private final SafAndroidCapabilityBridge bridge =
            new SafAndroidCapabilityBridge();
    private TextView output;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        output = new TextView(this);
        output.setText("SAF runtime bridge ready.\nSelect a host folder.");

        Button select = new Button(this);
        select.setText("SELECT HOST FOLDER (SAF)");
        select.setOnClickListener(view -> openTreePicker());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        content.addView(select, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(content);
    }

    private void openTreePicker() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, REQUEST_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_TREE) return;

        Uri treeUri = data == null ? null : data.getData();

        if (resultCode != RESULT_OK || treeUri == null) {
            showText("status=DENIED\nmessage=picker-cancelled");
            return;
        }

        if (!"content".equals(treeUri.getScheme())) {
            showText("status=DENIED\nmessage=content-uri-required");
            return;
        }

        if (!android.provider.DocumentsContract.isTreeUri(treeUri)) {
            showText("status=UNSUPPORTED\nmessage=tree-uri-required");
            return;
        }

        new Thread(() -> {
            SafAndroidCapabilityBridge.Result result =
                    bridge.createWriteRead(
                            this,
                            treeUri,
                            DISPLAY_NAME,
                            MIME_TYPE,
                            SafAndroidCapabilityBridge.PAYLOAD);

            String text = "status=" + result.getStatus()
                    + "\nbytesWritten=" + result.getBytesWritten()
                    + "\nbytesRead=" + result.getBytesRead()
                    + "\ncontentSha256=" + result.getContentSha256()
                    + "\npersistedRead=" + result.hasPersistedReadPermission()
                    + "\npersistedWrite=" + result.hasPersistedWritePermission()
                    + "\nmetadataVerified=" + result.isMetadataVerified()
                    + "\ncapabilityHandleHash=" + result.getCapabilityHandleHash()
                    + "\nmessage=" + result.getMessage()
                    + "\nevidence=" + result.toEvidenceJson();

            showText(text);
        }, "alfa-saf-runtime-test").start();
    }

    private void showText(String text) {
        runOnUiThread(() -> output.setText(text));
    }
}
