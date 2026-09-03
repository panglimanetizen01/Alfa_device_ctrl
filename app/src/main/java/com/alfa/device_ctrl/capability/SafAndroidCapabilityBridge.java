package com.alfa.device_ctrl.capability;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public final class SafAndroidCapabilityBridge implements AndroidCapabilityBridge {
    public static final String CAPABILITY = "storage.saf.roundtrip";
    public static final String PAYLOAD = "ALFA_SAF_PROBE_V1";

    public enum Status { PASS, DENIED, UNSUPPORTED, ERROR }

    public static final class Result {
        private final Status status;
        private final String message;
        private final Uri capabilityHandle;
        private final String capabilityHandleHash;
        private final long bytesWritten;
        private final long bytesRead;
        private final String contentSha256;
        private final boolean persistedRead;
        private final boolean persistedWrite;
        private final boolean metadataVerified;

        private Result(Status status, String message, Uri capabilityHandle,
                       long bytesWritten, long bytesRead, String contentSha256,
                       boolean persistedRead, boolean persistedWrite,
                       boolean metadataVerified) {
            this.status = status;
            this.message = message == null ? "" : message;
            this.capabilityHandle = capabilityHandle;
            this.capabilityHandleHash = sha256(
                    capabilityHandle == null ? "" : capabilityHandle.toString());
            this.bytesWritten = bytesWritten;
            this.bytesRead = bytesRead;
            this.contentSha256 = contentSha256 == null ? "" : contentSha256;
            this.persistedRead = persistedRead;
            this.persistedWrite = persistedWrite;
            this.metadataVerified = metadataVerified;
        }

        public static Result pass(Uri handle, long written, long read, String digest,
                                  boolean persistedRead, boolean persistedWrite) {
            return new Result(Status.PASS, "", handle, written, read, digest,
                    persistedRead, persistedWrite, true);
        }

        public static Result failure(Status status, String message) {
            return new Result(status, message, null, 0, 0, "", false, false, false);
        }

        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public Uri getCapabilityHandle() { return capabilityHandle; }
        public String getCapabilityHandleHash() { return capabilityHandleHash; }
        public long getBytesWritten() { return bytesWritten; }
        public long getBytesRead() { return bytesRead; }
        public String getContentSha256() { return contentSha256; }
        public boolean hasPersistedReadPermission() { return persistedRead; }
        public boolean hasPersistedWritePermission() { return persistedWrite; }
        public boolean isMetadataVerified() { return metadataVerified; }

        public String toEvidenceJson() {
            return "{"
                    + "\"status\":\"" + status + "\","
                    + "\"message\":\"" + json(message) + "\","
                    + "\"capabilityHandleHash\":\"" + capabilityHandleHash + "\","
                    + "\"bytesWritten\":" + bytesWritten + ","
                    + "\"bytesRead\":" + bytesRead + ","
                    + "\"contentSha256\":\"" + contentSha256 + "\","
                    + "\"persistedRead\":" + persistedRead + ","
                    + "\"persistedWrite\":" + persistedWrite + ","
                    + "\"metadataVerified\":" + metadataVerified
                    + "}";
        }
    }

    @Override
    public Result createWriteRead(Context context, Uri treeUri, String displayName,
                                  String mimeType, String payload) {
        if (context == null || treeUri == null)
            return Result.failure(Status.DENIED, "context-or-tree-uri-null");

        if (!"content".equals(treeUri.getScheme()))
            return Result.failure(Status.DENIED, "content-uri-required");

        if (!DocumentsContract.isTreeUri(treeUri))
            return Result.failure(Status.UNSUPPORTED, "tree-uri-required");

        if (displayName == null || displayName.length() == 0
                || mimeType == null || payload == null)
            return Result.failure(Status.ERROR, "invalid-request");

        ContentResolver resolver = context.getContentResolver();
        boolean persistedRead = false;
        boolean persistedWrite = false;

        try {
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

            try {
                resolver.takePersistableUriPermission(treeUri, flags);
            } catch (SecurityException ignored) {
            }

            for (UriPermission permission : resolver.getPersistedUriPermissions()) {
                if (treeUri.equals(permission.getUri())) {
                    persistedRead = permission.isReadPermission();
                    persistedWrite = permission.isWritePermission();
                    break;
                }
            }

            String treeDocumentId =
                    DocumentsContract.getTreeDocumentId(treeUri);

            Uri parentDocumentUri =
                    DocumentsContract.buildDocumentUriUsingTree(
                            treeUri, treeDocumentId);

            Uri fileUri = DocumentsContract.createDocument(
                    resolver, parentDocumentUri, mimeType, displayName);

            if (fileUri == null)
                return Result.failure(Status.UNSUPPORTED,
                        "provider-create-returned-null");

            byte[] expected =
                    payload.getBytes(StandardCharsets.UTF_8);

            try (OutputStream output =
                         resolver.openOutputStream(fileUri, "w")) {
                if (output == null)
                    return Result.failure(Status.ERROR,
                            "content-resolver-output-null");

                output.write(expected);
                output.flush();
            }

            byte[] actual;

            try (InputStream input =
                         resolver.openInputStream(fileUri)) {
                if (input == null)
                    return Result.failure(Status.ERROR,
                            "content-resolver-input-null");

                actual = readAll(input);
            }

            if (!Arrays.equals(expected, actual))
                return Result.failure(Status.ERROR,
                        "content-roundtrip-mismatch");

            if (!verifyMetadata(
                    resolver, fileUri, displayName, mimeType, actual.length))
                return Result.failure(Status.ERROR,
                        "metadata-verification-failed");

            return Result.pass(
                    fileUri,
                    expected.length,
                    actual.length,
                    sha256(actual),
                    persistedRead,
                    persistedWrite);

        } catch (SecurityException e) {
            return Result.failure(
                    Status.DENIED,
                    "security-exception:" + safeMessage(e));

        } catch (UnsupportedOperationException e) {
            return Result.failure(
                    Status.UNSUPPORTED,
                    "provider-unsupported:" + safeMessage(e));

        } catch (Exception e) {
            return Result.failure(
                    Status.ERROR,
                    e.getClass().getSimpleName() + ":" + safeMessage(e));
        }
    }

    private static boolean verifyMetadata(
            ContentResolver resolver,
            Uri fileUri,
            String expectedName,
            String expectedMime,
            long expectedSize) {

        String[] columns = {
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };

        try (Cursor cursor =
                     resolver.query(fileUri, columns, null, null, null)) {

            if (cursor == null || !cursor.moveToFirst())
                return false;

            int nameIndex =
                    cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME);

            int mimeIndex =
                    cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_MIME_TYPE);

            int sizeIndex =
                    cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_SIZE);

            if (nameIndex < 0 || mimeIndex < 0 || sizeIndex < 0)
                return false;

            String name = cursor.getString(nameIndex);
            String mime = cursor.getString(mimeIndex);
            long size = cursor.isNull(sizeIndex)
                    ? -1 : cursor.getLong(sizeIndex);

            return expectedName.equals(name)
                    && expectedMime.equals(mime)
                    && expectedSize == size;

        } catch (Exception ignored) {
            return false;
        }
    }

    private static byte[] readAll(InputStream input)
            throws java.io.IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int count;

        while ((count = input.read(buffer)) != -1)
            output.write(buffer, 0, count);

        return output.toByteArray();
    }

    public static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest(value);

            StringBuilder result = new StringBuilder(64);

            for (byte item : digest)
                result.append(String.format("%02x", item & 0xff));

            return result.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "sha256-unavailable", e);
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null
                ? ""
                : message.replace('\n', ' ')
                         .replace('\r', ' ');
    }
}
