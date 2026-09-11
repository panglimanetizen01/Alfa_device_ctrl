package com.alfa.device_ctrl;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical host-project access through SAF. It deliberately exposes content URIs only and never
 * treats a guessed /sdcard path as proof of host visibility.
 */
public final class SafHostStorageProvider implements HostStorageProvider {
    private static final String PREFS = "alfa_host_storage";
    private static final String TREE_URI = "tree_uri";
    private static final int MAX_TEXT_BYTES = 1_048_576;

    private final Context context;
    private final ContentResolver resolver;
    private final SharedPreferences prefs;

    public SafHostStorageProvider(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Intent createTreePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return intent;
    }

    public boolean persistTreeUri(Uri treeUri) {
        if (treeUri == null) return false;
        try {
            resolver.takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            prefs.edit().putString(TREE_URI, treeUri.toString()).apply();
            return hasPersistedAccess(treeUri);
        } catch (SecurityException error) {
            return false;
        }
    }

    public Uri treeUri() {
        String value = prefs.getString(TREE_URI, null);
        return value == null ? null : Uri.parse(value);
    }

    public boolean hasPersistedAccess() {
        Uri uri = treeUri();
        return uri != null && hasPersistedAccess(uri);
    }

    private boolean hasPersistedAccess(Uri uri) {
        for (android.content.UriPermission permission : resolver.getPersistedUriPermissions()) {
            if (uri.equals(permission.getUri()) && permission.isReadPermission() && permission.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    public List<Entry> list(Uri parentTreeOrDocumentUri) {
        if (parentTreeOrDocumentUri == null) return Collections.emptyList();
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
                parentTreeOrDocumentUri,
                DocumentsContract.getDocumentId(parentTreeOrDocumentUri));
        List<Entry> entries = new ArrayList<>();
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_FLAGS
        };
        try (Cursor cursor = resolver.query(children, projection, null, null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) return entries;
            int id = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int name = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int size = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE);
            int modified = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            int flags = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS);
            while (cursor.moveToNext()) {
                String documentId = cursor.getString(id);
                entries.add(new Entry(
                        DocumentsContract.buildDocumentUriUsingTree(parentTreeOrDocumentUri, documentId),
                        cursor.getString(name), cursor.getString(mime),
                        cursor.isNull(size) ? -1L : cursor.getLong(size),
                        cursor.isNull(modified) ? -1L : cursor.getLong(modified),
                        cursor.getInt(flags)));
            }
        }
        return entries;
    }

    public String readUtf8(Uri documentUri) throws Exception {
        if (documentUri == null) throw new IllegalArgumentException("documentUri");
        try (InputStream input = resolver.openInputStream(documentUri)) {
            if (input == null) throw new FileNotFoundException(documentUri.toString());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_TEXT_BYTES) throw new IllegalArgumentException("file-too-large");
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public void writeUtf8(Uri documentUri, String content) throws Exception {
        if (documentUri == null) throw new IllegalArgumentException("documentUri");
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_BYTES) throw new IllegalArgumentException("file-too-large");
        try (OutputStream output = resolver.openOutputStream(documentUri, "wt")) {
            if (output == null) throw new FileNotFoundException(documentUri.toString());
            output.write(bytes);
            output.flush();
        }
    }

    public Uri createDirectory(Uri parentTreeOrDocumentUri, String name) throws Exception {
        validateName(name);
        return DocumentsContract.createDocument(resolver, parentTreeOrDocumentUri,
                DocumentsContract.Document.MIME_TYPE_DIR, name);
    }

    public Uri createTextFile(Uri parentTreeOrDocumentUri, String name) throws Exception {
        validateName(name);
        return DocumentsContract.createDocument(resolver, parentTreeOrDocumentUri,
                "text/plain", name);
    }

    public boolean delete(Uri documentUri) throws Exception {
        if (documentUri == null) return false;
        return DocumentsContract.deleteDocument(resolver, documentUri);
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty() || name.contains("/") || name.contains("\\")
                || ".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException("invalid-document-name");
        }
    }

    public static final class Entry {
        public final Uri uri;
        public final String name;
        public final String mimeType;
        public final long size;
        public final long modifiedEpochMs;
        public final int flags;

        Entry(Uri uri, String name, String mimeType, long size, long modifiedEpochMs, int flags) {
            this.uri = uri;
            this.name = name;
            this.mimeType = mimeType;
            this.size = size;
            this.modifiedEpochMs = modifiedEpochMs;
            this.flags = flags;
        }

        public boolean isDirectory() {
            return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
        }
    }
}
