package com.alfa.device_ctrl;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists only provider metadata required to rediscover a selected SAF tree.
 *
 * <p>This class does not scan raw storage, create provider documents, write
 * artifact bytes, or verify host visibility. It is intentionally limited to
 * the selection record needed by later export steps.</p>
 */
public final class SafTreeDestinationStore {
    private static final String PREFERENCES = "alfa_host_storage_bridge";
    private static final String KEY_REQUEST_ID = "request_id";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String KEY_PROVIDER_AUTHORITY = "provider_authority";
    private static final String KEY_DOCUMENT_ID = "document_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PERMISSION_FLAGS = "persisted_permission_flags";
    private static final String KEY_VERIFICATION_STATE = "verification_state";
    private static final String KEY_RECORDED_AT = "recorded_at_epoch_millis";

    public boolean save(Context context, SafTreeDestinationRecord record) {
        if (context == null) throw new IllegalArgumentException("context is required");
        if (record == null) throw new IllegalArgumentException("record is required");
        return preferences(context).edit()
                .putString(KEY_REQUEST_ID, record.requestId())
                .putString(KEY_TREE_URI, record.treeUri())
                .putString(KEY_PROVIDER_AUTHORITY, record.providerAuthority())
                .putString(KEY_DOCUMENT_ID, record.documentId())
                .putString(KEY_DISPLAY_NAME, record.displayName())
                .putInt(KEY_PERMISSION_FLAGS, record.persistedPermissionFlags())
                .putString(KEY_VERIFICATION_STATE, record.verificationState().name())
                .putLong(KEY_RECORDED_AT, record.recordedAtEpochMillis())
                .commit();
    }

    public SafTreeDestinationRecord load(Context context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        SharedPreferences values = preferences(context);
        String requestId = values.getString(KEY_REQUEST_ID, null);
        String treeUri = values.getString(KEY_TREE_URI, null);
        String authority = values.getString(KEY_PROVIDER_AUTHORITY, null);
        String documentId = values.getString(KEY_DOCUMENT_ID, null);
        String displayName = values.getString(KEY_DISPLAY_NAME, null);
        String verificationName = values.getString(KEY_VERIFICATION_STATE, null);
        if (requestId == null || treeUri == null || authority == null
                || documentId == null || displayName == null || verificationName == null) {
            return null;
        }
        try {
            HostStorageExportContract.VerificationState verificationState =
                    HostStorageExportContract.VerificationState.valueOf(verificationName);
            return new SafTreeDestinationRecord(
                    requestId,
                    treeUri,
                    authority,
                    documentId,
                    displayName,
                    values.getInt(KEY_PERMISSION_FLAGS, 0),
                    verificationState,
                    values.getLong(KEY_RECORDED_AT, 0L));
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public boolean clear(Context context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return preferences(context).edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFERENCES,
                Context.MODE_PRIVATE);
    }
}
