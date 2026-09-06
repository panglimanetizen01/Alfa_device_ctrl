package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.util.List;

/**
 * Android SAF boundary for selecting and persisting a tree destination.
 *
 * <p>This class only selects and records a provider destination. It does not
 * create directories, write files, export bytes, verify hashes, or claim host
 * visibility.</p>
 */
public final class SafTreeDestinationSelector {
    public static final int REQUEST_CODE = 0xA17A;
    public static final int REQUIRED_PERSISTABLE_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private SafTreeDestinationSelector() {
        // Utility class.
    }

    public static Intent createSelectionIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return intent;
    }

    public static SelectionResult handleActivityResult(
            int resultCode,
            Intent resultIntent,
            ContentResolver resolver,
            String requestId,
            String displayName) {
        if (resultCode != Activity.RESULT_OK || resultIntent == null) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.CANCELLED,
                    "export-provider-selection-cancelled",
                    "Android storage selection was cancelled",
                    "Select an Android storage folder to continue");
        }
        if (resolver == null) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.FAILED_PROVIDER_UNAVAILABLE,
                    "export-provider-unavailable",
                    "Android ContentResolver is unavailable",
                    "Retry after selecting a host storage location");
        }
        Uri treeUri = resultIntent.getData();
        if (!isValidTreeUri(treeUri)) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.FAILED_PROVIDER_UNAVAILABLE,
                    "export-invalid-tree-uri",
                    "Selected destination is not a canonical content tree URI",
                    "Choose a folder from the Android storage picker");
        }

        int grantedFlags = resultIntent.getFlags() & REQUIRED_PERSISTABLE_FLAGS;
        if ((grantedFlags & REQUIRED_PERSISTABLE_FLAGS) != REQUIRED_PERSISTABLE_FLAGS) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.FAILED_PERMISSION_DENIED,
                    "export-persistable-permission-denied",
                    "Selected provider did not grant persistable read/write access",
                    "Choose another Android storage provider or folder");
        }

        try {
            resolver.takePersistableUriPermission(treeUri, grantedFlags);
        } catch (SecurityException error) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.FAILED_PERMISSION_DENIED,
                    "export-persistable-permission-denied",
                    "Android denied persistable access to the selected provider URI",
                    "Choose another Android storage provider or folder");
        }

        if (!hasPersistedFlags(resolver, treeUri, REQUIRED_PERSISTABLE_FLAGS)) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.FAILED_PERMISSION_DENIED,
                    "export-persisted-permission-not-confirmed",
                    "Persisted provider read/write permission could not be confirmed",
                    "Choose another Android storage provider or folder");
        }

        try {
            String authority = treeUri.getAuthority();
            String documentId = DocumentsContract.getTreeDocumentId(treeUri);
            SafTreeDestinationRecord record = new SafTreeDestinationRecord(
                    requestId,
                    treeUri.toString(),
                    authority,
                    documentId,
                    displayName,
                    grantedFlags,
                    HostStorageExportContract.VerificationState.NOT_VERIFIED,
                    System.currentTimeMillis());
            return SelectionResult.success(record);
        } catch (RuntimeException error) {
            return SelectionResult.failure(
                    HostStorageExportContract.ExportState.FAILED_PROVIDER_UNAVAILABLE,
                    "export-provider-metadata-invalid",
                    "Selected provider metadata could not be read",
                    "Choose another Android storage provider or folder");
        }
    }

    private static boolean isValidTreeUri(Uri uri) {
        return uri != null
                && "content".equalsIgnoreCase(uri.getScheme())
                && uri.getAuthority() != null
                && !uri.getAuthority().trim().isEmpty()
                && DocumentsContract.isTreeUri(uri);
    }

    private static boolean hasPersistedFlags(
            ContentResolver resolver,
            Uri treeUri,
            int requiredFlags) {
        List<UriPermission> permissions = resolver.getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            if (treeUri.equals(permission.getUri())
                    && ((requiredFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0 || permission.isReadPermission())
                    && ((requiredFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0 || permission.isWritePermission())) {
                return true;
            }
        }
        return false;
    }

    public static final class SelectionResult {
        private final boolean success;
        private final HostStorageExportContract.ExportState state;
        private final SafTreeDestinationRecord record;
        private final String errorCode;
        private final String errorMessage;
        private final String actionableNextStep;

        private SelectionResult(
                boolean success,
                HostStorageExportContract.ExportState state,
                SafTreeDestinationRecord record,
                String errorCode,
                String errorMessage,
                String actionableNextStep) {
            this.success = success;
            this.state = state;
            this.record = record;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.actionableNextStep = actionableNextStep;
        }

        public static SelectionResult success(SafTreeDestinationRecord record) {
            if (record == null) throw new IllegalArgumentException("record is required");
            return new SelectionResult(
                    true,
                    HostStorageExportContract.ExportState.HOST_PROVIDER_SELECTED,
                    record,
                    "",
                    "",
                    "");
        }

        public static SelectionResult failure(
                HostStorageExportContract.ExportState state,
                String errorCode,
                String errorMessage,
                String actionableNextStep) {
            if (state == HostStorageExportContract.ExportState.HOST_PROVIDER_SELECTED) {
                throw new IllegalArgumentException("failure cannot use success state");
            }
            return new SelectionResult(
                    false,
                    state,
                    null,
                    errorCode,
                    errorMessage,
                    actionableNextStep);
        }

        public boolean success() {
            return success;
        }

        public HostStorageExportContract.ExportState state() {
            return state;
        }

        public SafTreeDestinationRecord record() {
            return record;
        }

        public String errorCode() {
            return errorCode;
        }

        public String errorMessage() {
            return errorMessage;
        }

        public String actionableNextStep() {
            return actionableNextStep;
        }
    }
}
