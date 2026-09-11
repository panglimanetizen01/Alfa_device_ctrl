package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Real system SAF directory picker. Persists only the user-selected tree URI. */
public final class StoragePickerActivity extends Activity {
    private static final int PICK_TREE = 7105;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, PICK_TREE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TREE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { getContentResolver().takePersistableUriPermission(uri, flags); } catch (SecurityException ignored) { }
            getPreferences(MODE_PRIVATE).edit().putString("selected_tree_uri", uri.toString()).apply();
            getSharedPreferences("alfa_storage", MODE_PRIVATE).edit().putString("tree_uri", uri.toString()).apply();
        }
        finish();
    }
}
