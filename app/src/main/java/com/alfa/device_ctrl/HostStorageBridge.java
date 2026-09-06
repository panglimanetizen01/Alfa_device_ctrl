package com.alfa.device_ctrl;

import java.util.Objects;

/**
 * Provider-neutral boundary for host-visible artifact export.
 *
 * <p>This interface intentionally contains no SAF, MediaStore, ContentResolver,
 * Android URI, filesystem writer, or rediscovery implementation. Those concerns
 * belong to later provider and persistence steps.</p>
 */
public interface HostStorageBridge {
    /**
     * Starts an export operation for a validated request.
     *
     * <p>The implementation owns provider selection, writing, finalization, and
     * verification. It must never treat a raw Linux path or UserLAnd FUSE path
     * as host visibility evidence.</p>
     */
    ExportSession start(
            HostStorageExportContract.ExportRequest request,
            ProgressListener progressListener) throws HostStorageException;

    /**
     * Reports an export operation without exposing provider-specific APIs.
     */
    interface ExportSession {
        String requestId();

        HostStorageExportContract.ExportRequest request();

        HostStorageExportContract.ExportResult snapshot();

        void cancel();
    }

    /**
     * Receives state/progress only; it performs no provider operation.
     */
    interface ProgressListener {
        void onProgress(
                HostStorageExportContract.ExportState state,
                long filesProcessed,
                long bytesProcessed);
    }

    /**
     * Structured failure crossing the provider-neutral boundary.
     */
    final class HostStorageException extends Exception {
        public enum Code {
            PROVIDER_UNAVAILABLE,
            PERMISSION_DENIED,
            WRITE_FAILED,
            STORAGE_FULL,
            INTERRUPTED,
            FINALIZATION_FAILED,
            VERIFICATION_FAILED,
            HOST_VISIBILITY_FAILED,
            NAME_CONFLICT
        }

        private final Code code;

        public HostStorageException(Code code, String message) {
            super(message);
            this.code = Objects.requireNonNull(code, "code");
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("message is required");
            }
        }

        public HostStorageException(Code code, String message, Throwable cause) {
            super(message, cause);
            this.code = Objects.requireNonNull(code, "code");
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("message is required");
            }
        }

        public Code code() {
            return code;
        }
    }
}
