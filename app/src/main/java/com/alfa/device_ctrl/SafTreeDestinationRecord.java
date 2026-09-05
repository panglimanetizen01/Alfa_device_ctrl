package com.alfa.device_ctrl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Provider metadata for a user-selected SAF tree destination.
 *
 * <p>This value object does not access Android APIs or the filesystem. The
 * Android selector/store layers supply and persist its provider-derived fields.</p>
 */
public final class SafTreeDestinationRecord {
    private final String requestId;
    private final String treeUri;
    private final String providerAuthority;
    private final String documentId;
    private final String displayName;
    private final int persistedPermissionFlags;
    private final HostStorageExportContract.VerificationState verificationState;
    private final long recordedAtEpochMillis;

    public SafTreeDestinationRecord(
            String requestId,
            String treeUri,
            String providerAuthority,
            String documentId,
            String displayName,
            int persistedPermissionFlags,
            HostStorageExportContract.VerificationState verificationState,
            long recordedAtEpochMillis) {
        this.requestId = required(requestId, "requestId");
        this.treeUri = requiredContentUri(treeUri);
        this.providerAuthority = required(providerAuthority, "providerAuthority");
        this.documentId = required(documentId, "documentId");
        this.displayName = required(displayName, "displayName");
        if (persistedPermissionFlags == 0) {
            throw new IllegalArgumentException("persistedPermissionFlags is required");
        }
        if (recordedAtEpochMillis < 0) {
            throw new IllegalArgumentException("recordedAtEpochMillis must be non-negative");
        }
        if (!providerAuthority.equals(authorityOf(treeUri))) {
            throw new IllegalArgumentException("providerAuthority does not match treeUri authority");
        }
        this.persistedPermissionFlags = persistedPermissionFlags;
        this.verificationState = Objects.requireNonNull(verificationState, "verificationState");
        this.recordedAtEpochMillis = recordedAtEpochMillis;
    }

    public String requestId() {
        return requestId;
    }

    public String treeUri() {
        return treeUri;
    }

    public String providerAuthority() {
        return providerAuthority;
    }

    public String documentId() {
        return documentId;
    }

    public String displayName() {
        return displayName;
    }

    public int persistedPermissionFlags() {
        return persistedPermissionFlags;
    }

    public HostStorageExportContract.VerificationState verificationState() {
        return verificationState;
    }

    public long recordedAtEpochMillis() {
        return recordedAtEpochMillis;
    }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String requiredContentUri(String value) {
        String uri = required(value, "treeUri");
        try {
            URI parsed = new URI(uri);
            if (!"content".equalsIgnoreCase(parsed.getScheme())
                    || parsed.getAuthority() == null
                    || parsed.getAuthority().trim().isEmpty()) {
                throw new IllegalArgumentException("treeUri must be canonical content:// URI");
            }
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("treeUri is invalid", error);
        }
        return uri;
    }

    private static String authorityOf(String value) {
        try {
            return new URI(value).getAuthority();
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("treeUri is invalid", error);
        }
    }
}
