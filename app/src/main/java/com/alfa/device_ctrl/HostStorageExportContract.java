package com.alfa.device_ctrl;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Step 1 contract for host-visible artifact export.
 *
 * <p>This class is deliberately Android-free. SAF, MediaStore, ContentResolver,
 * and provider-specific operations belong to the later HostStorageBridge step.</p>
 */
public final class HostStorageExportContract {
    private HostStorageExportContract() {
        // Contract namespace.
    }

    public enum ArtifactType {
        APK,
        PROJECT_ARCHIVE,
        SOURCE_EXPORT,
        GENERATED_ARTIFACT,
        LOG,
        REPORT,
        OTHER
    }

    public enum DestinationMode {
        SAF_TREE,
        MEDIASTORE_DOWNLOADS
    }

    public enum OverwritePolicy {
        NEVER_SILENT,
        RENAME_IF_CONFLICT,
        REPLACE_WITH_EXPLICIT_CONFIRMATION
    }

    public enum VerificationState {
        NOT_VERIFIED,
        PROVIDER_VERIFIED,
        HOST_VISIBLE,
        FAILED
    }

    public enum ExportState {
        EXPORT_REQUESTED,
        HOST_PROVIDER_SELECTED,
        WRITING,
        FINALIZED,
        VERIFIED,
        USER_VISIBLE,
        CANCELLED,
        FAILED_PROVIDER_UNAVAILABLE,
        FAILED_PERMISSION_DENIED,
        FAILED_WRITE,
        FAILED_STORAGE_FULL,
        FAILED_INTERRUPTED,
        FAILED_FINALIZATION,
        FAILED_VERIFICATION,
        FAILED_HOST_VISIBILITY,
        FAILED_NAME_CONFLICT
    }

    public static final class ExportRequest {
        private final String requestId;
        private final ArtifactType artifactType;
        private final String sourceDescriptor;
        private final String displayName;
        private final String mimeType;
        private final DestinationMode destinationMode;
        private final String requestedDestination;
        private final OverwritePolicy overwritePolicy;
        private final long expectedFileCount;
        private final long expectedByteSize;
        private final String expectedSha256;

        private ExportRequest(Builder builder) {
            this.requestId = required(builder.requestId, "requestId");
            this.artifactType = Objects.requireNonNull(builder.artifactType, "artifactType");
            this.sourceDescriptor = required(builder.sourceDescriptor, "sourceDescriptor");
            this.displayName = required(builder.displayName, "displayName");
            this.mimeType = required(builder.mimeType, "mimeType");
            this.destinationMode = Objects.requireNonNull(builder.destinationMode, "destinationMode");
            this.requestedDestination = required(builder.requestedDestination, "requestedDestination");
            this.overwritePolicy = Objects.requireNonNull(builder.overwritePolicy, "overwritePolicy");
            if (builder.expectedFileCount < 0) {
                throw new IllegalArgumentException("expectedFileCount must be non-negative");
            }
            if (builder.expectedByteSize < 0) {
                throw new IllegalArgumentException("expectedByteSize must be non-negative");
            }
            this.expectedFileCount = builder.expectedFileCount;
            this.expectedByteSize = builder.expectedByteSize;
            this.expectedSha256 = builder.expectedSha256 == null
                    ? ""
                    : normalizedSha256(builder.expectedSha256);
        }

        public static Builder builder() {
            return new Builder();
        }

        public String requestId() {
            return requestId;
        }

        public ArtifactType artifactType() {
            return artifactType;
        }

        public String sourceDescriptor() {
            return sourceDescriptor;
        }

        public String displayName() {
            return displayName;
        }

        public String mimeType() {
            return mimeType;
        }

        public DestinationMode destinationMode() {
            return destinationMode;
        }

        public String requestedDestination() {
            return requestedDestination;
        }

        public OverwritePolicy overwritePolicy() {
            return overwritePolicy;
        }

        public long expectedFileCount() {
            return expectedFileCount;
        }

        public long expectedByteSize() {
            return expectedByteSize;
        }

        public String expectedSha256() {
            return expectedSha256;
        }

        public static final class Builder {
            private String requestId;
            private ArtifactType artifactType;
            private String sourceDescriptor;
            private String displayName;
            private String mimeType;
            private DestinationMode destinationMode;
            private String requestedDestination;
            private OverwritePolicy overwritePolicy = OverwritePolicy.NEVER_SILENT;
            private long expectedFileCount;
            private long expectedByteSize;
            private String expectedSha256;

            public Builder requestId(String value) {
                requestId = value;
                return this;
            }

            public Builder artifactType(ArtifactType value) {
                artifactType = value;
                return this;
            }

            public Builder sourceDescriptor(String value) {
                sourceDescriptor = value;
                return this;
            }

            public Builder displayName(String value) {
                displayName = value;
                return this;
            }

            public Builder mimeType(String value) {
                mimeType = value;
                return this;
            }

            public Builder destinationMode(DestinationMode value) {
                destinationMode = value;
                return this;
            }

            public Builder requestedDestination(String value) {
                requestedDestination = value;
                return this;
            }

            public Builder overwritePolicy(OverwritePolicy value) {
                overwritePolicy = value;
                return this;
            }

            public Builder expectedFileCount(long value) {
                expectedFileCount = value;
                return this;
            }

            public Builder expectedByteSize(long value) {
                expectedByteSize = value;
                return this;
            }

            public Builder expectedSha256(String value) {
                expectedSha256 = value;
                return this;
            }

            public ExportRequest build() {
                return new ExportRequest(this);
            }
        }
    }

    public static final class ExportResult {
        private final String requestId;
        private final ExportState state;
        private final VerificationState verificationState;
        private final String providerName;
        private final String documentId;
        private final String uri;
        private final String displayName;
        private final String mimeType;
        private final long byteSize;
        private final long fileCount;
        private final String sha256;
        private final String destinationLabel;
        private final String errorCode;
        private final String errorMessage;
        private final String actionableNextStep;

        private ExportResult(Builder builder) {
            this.requestId = required(builder.requestId, "requestId");
            this.state = Objects.requireNonNull(builder.state, "state");
            this.verificationState = Objects.requireNonNull(builder.verificationState, "verificationState");
            this.providerName = valueOrEmpty(builder.providerName);
            this.documentId = valueOrEmpty(builder.documentId);
            this.uri = valueOrEmpty(builder.uri);
            this.displayName = valueOrEmpty(builder.displayName);
            this.mimeType = valueOrEmpty(builder.mimeType);
            this.byteSize = nonNegative(builder.byteSize, "byteSize");
            this.fileCount = nonNegative(builder.fileCount, "fileCount");
            this.sha256 = builder.sha256 == null ? "" : normalizedSha256(builder.sha256);
            this.destinationLabel = valueOrEmpty(builder.destinationLabel);
            this.errorCode = valueOrEmpty(builder.errorCode);
            this.errorMessage = valueOrEmpty(builder.errorMessage);
            this.actionableNextStep = valueOrEmpty(builder.actionableNextStep);
            validateErrorConsistency();
        }

        public static Builder builder() {
            return new Builder();
        }

        public String requestId() {
            return requestId;
        }

        public ExportState state() {
            return state;
        }

        public VerificationState verificationState() {
            return verificationState;
        }

        public String providerName() {
            return providerName;
        }

        public String documentId() {
            return documentId;
        }

        public String uri() {
            return uri;
        }

        public String displayName() {
            return displayName;
        }

        public String mimeType() {
            return mimeType;
        }

        public long byteSize() {
            return byteSize;
        }

        public long fileCount() {
            return fileCount;
        }

        public String sha256() {
            return sha256;
        }

        public String destinationLabel() {
            return destinationLabel;
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

        private void validateErrorConsistency() {
            if (isFailure(state) && errorMessage.isEmpty()) {
                throw new IllegalArgumentException("failure result requires errorMessage");
            }
            if (state == ExportState.USER_VISIBLE && verificationState != VerificationState.HOST_VISIBLE) {
                throw new IllegalArgumentException("USER_VISIBLE requires HOST_VISIBLE verificationState");
            }
            if (state == ExportState.VERIFIED && verificationState == VerificationState.NOT_VERIFIED) {
                throw new IllegalArgumentException("VERIFIED requires provider verification");
            }
        }

        public static final class Builder {
            private String requestId;
            private ExportState state;
            private VerificationState verificationState = VerificationState.NOT_VERIFIED;
            private String providerName;
            private String documentId;
            private String uri;
            private String displayName;
            private String mimeType;
            private long byteSize;
            private long fileCount;
            private String sha256;
            private String destinationLabel;
            private String errorCode;
            private String errorMessage;
            private String actionableNextStep;

            public Builder requestId(String value) {
                requestId = value;
                return this;
            }

            public Builder state(ExportState value) {
                state = value;
                return this;
            }

            public Builder verificationState(VerificationState value) {
                verificationState = value;
                return this;
            }

            public Builder providerName(String value) {
                providerName = value;
                return this;
            }

            public Builder documentId(String value) {
                documentId = value;
                return this;
            }

            public Builder uri(String value) {
                uri = value;
                return this;
            }

            public Builder displayName(String value) {
                displayName = value;
                return this;
            }

            public Builder mimeType(String value) {
                mimeType = value;
                return this;
            }

            public Builder byteSize(long value) {
                byteSize = value;
                return this;
            }

            public Builder fileCount(long value) {
                fileCount = value;
                return this;
            }

            public Builder sha256(String value) {
                sha256 = value;
                return this;
            }

            public Builder destinationLabel(String value) {
                destinationLabel = value;
                return this;
            }

            public Builder errorCode(String value) {
                errorCode = value;
                return this;
            }

            public Builder errorMessage(String value) {
                errorMessage = value;
                return this;
            }

            public Builder actionableNextStep(String value) {
                actionableNextStep = value;
                return this;
            }

            public ExportResult build() {
                return new ExportResult(this);
            }
        }
    }

    public static final class StateMachine {
        private ExportState state = ExportState.EXPORT_REQUESTED;

        public ExportState state() {
            return state;
        }

        public boolean isTerminal() {
            return state == ExportState.USER_VISIBLE || state == ExportState.CANCELLED || isFailure(state);
        }

        public void transition(ExportState next) {
            Objects.requireNonNull(next, "next");
            if (!isAllowed(state, next)) {
                throw new IllegalStateException("invalid export transition: " + state + " -> " + next);
            }
            state = next;
        }

        public void fail(ExportState failureState, String errorMessage) {
            if (!isFailure(failureState)) {
                throw new IllegalArgumentException("not a failure state: " + failureState);
            }
            if (isTerminal()) {
                throw new IllegalStateException("export state is already terminal: " + state);
            }
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("failure requires errorMessage");
            }
            state = failureState;
        }

        public void cancel() {
            if (isTerminal()) {
                throw new IllegalStateException("export state is already terminal: " + state);
            }
            state = ExportState.CANCELLED;
        }
    }

    private static boolean isAllowed(ExportState current, ExportState next) {
        switch (current) {
            case EXPORT_REQUESTED:
                return next == ExportState.HOST_PROVIDER_SELECTED;
            case HOST_PROVIDER_SELECTED:
                return next == ExportState.WRITING;
            case WRITING:
                return next == ExportState.FINALIZED;
            case FINALIZED:
                return next == ExportState.VERIFIED;
            case VERIFIED:
                return next == ExportState.USER_VISIBLE;
            default:
                return false;
        }
    }

    private static boolean isFailure(ExportState state) {
        return state == ExportState.FAILED_PROVIDER_UNAVAILABLE
                || state == ExportState.FAILED_PERMISSION_DENIED
                || state == ExportState.FAILED_WRITE
                || state == ExportState.FAILED_STORAGE_FULL
                || state == ExportState.FAILED_INTERRUPTED
                || state == ExportState.FAILED_FINALIZATION
                || state == ExportState.FAILED_VERIFICATION
                || state == ExportState.FAILED_HOST_VISIBILITY
                || state == ExportState.FAILED_NAME_CONFLICT;
    }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    private static String normalizedSha256(String value) {
        String normalized = required(value, "sha256").toLowerCase(Locale.US);
        if (!Pattern.matches("[0-9a-f]{64}", normalized)) {
            throw new IllegalArgumentException("sha256 must be exactly 64 hexadecimal characters");
        }
        return normalized;
    }
}
