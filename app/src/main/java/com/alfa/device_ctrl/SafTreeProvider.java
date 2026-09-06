package com.alfa.device_ctrl;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Real SAF tree provider implementation for Step B.
 *
 * <p>This class owns only provider-side destination selection output, staging
 * directory creation, nested document creation, and bounded byte writing. Full
 * read-back verification, manifest/hash verification, rediscovery, MediaStore,
 * and host-visible verification are intentionally deferred to later steps.</p>
 */
public final class SafTreeProvider implements HostStorageProvider {
    private static final Pattern SHA_SAFE_NAME = Pattern.compile("[^\\p{Cntrl}]{1,240}");
    private static final int BUFFER_SIZE = 32 * 1024;

    private final ContentResolver resolver;

    public SafTreeProvider(ContentResolver resolver) {
        if (resolver == null) throw new IllegalArgumentException("resolver is required");
        this.resolver = resolver;
    }

    @Override
    public String providerName() {
        return "saf";
    }

    @Override
    public boolean supports(HostStorageExportContract.DestinationMode destinationMode) {
        return destinationMode == HostStorageExportContract.DestinationMode.SAF_TREE;
    }

    @Override
    public ExportSession begin(
            HostStorageExportContract.ExportRequest request,
            ProviderDestination destination) throws HostStorageBridge.HostStorageException {
        if (request == null) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "export-request-missing", "Export request is required", "Provide a valid export request");
        }
        if (!supports(request.destinationMode())) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-destination-mode-unsupported", "SAF provider requires SAF_TREE destination mode",
                    "Select an Android storage folder through SAF");
        }
        ValidatedDestination validated = validateDestination(destination);
        Uri treeUri = Uri.parse(validated.canonicalUri);
        Uri parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri, validated.documentId);
        String stagingName = stagingName(request.requestId());
        Uri stagingUri;
        try {
            if (findChildByName(parentDocumentUri, stagingName) != null) {
                throw failure(HostStorageBridge.HostStorageException.Code.NAME_CONFLICT,
                        "saf-staging-name-conflict", "Provider staging directory already exists",
                        "Retry the export with a new request ID");
            }
            stagingUri = DocumentsContract.createDocument(
                    resolver, parentDocumentUri, Document.MIME_TYPE_DIR, stagingName);
            if (stagingUri == null) {
                throw failure(HostStorageBridge.HostStorageException.Code.FINALIZATION_FAILED,
                        "saf-staging-create-failed", "SAF provider did not create the staging directory",
                        "Choose another Android storage folder");
            }
        } catch (HostStorageBridge.HostStorageException error) {
            throw error;
        } catch (SecurityException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PERMISSION_DENIED,
                    "saf-staging-permission-denied", "Android denied SAF staging access",
                    "Choose another Android storage folder");
        } catch (FileNotFoundException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-provider-unavailable", "SAF provider could not create a staging directory",
                    "Choose another Android storage provider");
        } catch (RuntimeException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-provider-error", "SAF provider rejected the selected tree",
                    "Choose another Android storage folder");
        }
        return new SafTreeExportSession(request, validated, stagingUri);
    }

    /**
     * Validates a provider-relative path without touching the filesystem.
     */
    public static String[] validateRelativePath(String relativePath, boolean directory)
            throws HostStorageBridge.HostStorageException {
        if (relativePath == null || relativePath.isEmpty()) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-relative-path-empty", "Relative path is empty", "Provide a relative provider path");
        }
        if (relativePath.indexOf('\0') >= 0) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-relative-path-nul", "Relative path contains NUL", "Remove NUL from the path");
        }
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")
                || relativePath.matches("^[A-Za-z]:[\\\\/].*")) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-relative-path-absolute", "Absolute paths are not allowed", "Use a relative export path");
        }
        String normalizedSeparators = relativePath.replace('\\', '/');
        String[] rawParts = normalizedSeparators.split("/", -1);
        if (rawParts.length == 0) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-relative-path-empty", "Relative path is empty", "Provide a relative provider path");
        }
        for (String part : rawParts) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                        "saf-relative-path-traversal", "Relative path contains traversal or empty segment",
                        "Use normalized relative path segments");
            }
            if (!SHA_SAFE_NAME.matcher(part).matches()) {
                throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                        "saf-relative-path-invalid-segment", "Relative path contains an invalid segment",
                        "Use provider-safe file and directory names");
            }
        }
        if (!directory && rawParts.length == 0) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-relative-path-invalid-file", "File path is invalid", "Provide a relative file path");
        }
        return rawParts;
    }

    public static String stagingName(String requestId) throws HostStorageBridge.HostStorageException {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-request-id-missing", "Request ID is required for provider staging",
                    "Create a unique export request ID");
        }
        String safe = requestId.trim();
        if (!safe.matches("[A-Za-z0-9._-]{1,180}")) {
            throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                    "saf-request-id-invalid", "Request ID contains unsafe characters",
                    "Use a provider-safe request ID");
        }
        return ".alfa-export-staging-" + safe;
    }

    private ValidatedDestination validateDestination(ProviderDestination destination)
            throws HostStorageBridge.HostStorageException {
        if (destination == null) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-destination-missing", "Provider destination is required",
                    "Select an Android storage folder through SAF");
        }
        String canonicalUri = required(destination.canonicalUri(), "saf-destination-uri-missing");
        Uri uri;
        try {
            uri = Uri.parse(canonicalUri);
        } catch (RuntimeException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-destination-uri-invalid", "Provider destination URI is invalid",
                    "Select an Android storage folder through SAF");
        }
        if (!"content".equalsIgnoreCase(uri.getScheme())
                || uri.getAuthority() == null
                || !DocumentsContract.isTreeUri(uri)) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-destination-uri-not-tree", "Destination is not a canonical SAF tree URI",
                    "Select a folder from the Android storage picker");
        }
        String documentId = required(destination.documentId(), "saf-document-id-missing");
        String providerName = required(destination.providerName(), "saf-provider-name-missing");
        if (!uri.getAuthority().equals(providerName)) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-provider-authority-mismatch", "Provider authority does not match the tree URI",
                    "Select a destination from the same Android storage provider");
        }
        String treeDocumentId;
        try {
            treeDocumentId = DocumentsContract.getTreeDocumentId(uri);
        } catch (RuntimeException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-tree-document-id-invalid", "SAF tree document ID could not be read",
                    "Select another Android storage folder");
        }
        if (!documentId.equals(treeDocumentId)) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-document-id-mismatch", "Destination document ID does not match tree URI",
                    "Select another Android storage folder");
        }
        String displayName = required(destination.displayName(), "saf-display-name-missing");
        return new ValidatedDestination(canonicalUri, documentId, providerName, displayName);
    }

    private Uri findChildByName(Uri parentDocumentUri, String displayName)
            throws HostStorageBridge.HostStorageException {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                parentDocumentUri, DocumentsContract.getDocumentId(parentDocumentUri));
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    childrenUri,
                    new String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME},
                    null,
                    null,
                    null);
            if (cursor == null) {
                throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                        "saf-child-query-null", "SAF provider returned no child cursor",
                        "Choose another Android storage provider");
            }
            int nameIndex = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME);
            int idIndex = cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID);
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && idIndex >= 0 && displayName.equals(cursor.getString(nameIndex))) {
                    return DocumentsContract.buildDocumentUriUsingTree(
                            parentDocumentUri, cursor.getString(idIndex));
                }
            }
            return null;
        } catch (HostStorageBridge.HostStorageException error) {
            throw error;
        } catch (SecurityException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PERMISSION_DENIED,
                    "saf-child-query-permission-denied", "Android denied SAF child enumeration",
                    "Choose another Android storage folder");
        } catch (RuntimeException error) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    "saf-child-query-failed", "SAF child enumeration failed",
                    "Choose another Android storage provider");
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String required(String value, String code)
            throws HostStorageBridge.HostStorageException {
        if (value == null || value.trim().isEmpty()) {
            throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                    code, "Required provider metadata is missing", "Select the destination again");
        }
        return value.trim();
    }

    private static HostStorageBridge.HostStorageException failure(
            HostStorageBridge.HostStorageException.Code code,
            String errorCode,
            String message,
            String nextStep) {
        return new HostStorageBridge.HostStorageException(code,
                errorCode + ": " + message + ". Next: " + nextStep);
    }

    private final class SafTreeExportSession implements HostStorageProvider.ExportSession {
        private final HostStorageExportContract.ExportRequest request;
        private final ValidatedDestination destination;
        private final Uri stagingUri;
        private final HostStorageExportContract.StateMachine machine =
                new HostStorageExportContract.StateMachine();
        private final Map<String, Uri> directories = new HashMap<>();
        private final Set<String> normalizedPaths = new HashSet<>();
        private HostStorageExportContract.ExportResult snapshot;
        private boolean cancelled;
        private long filesProcessed;
        private long bytesProcessed;

        private SafTreeExportSession(
                HostStorageExportContract.ExportRequest request,
                ValidatedDestination destination,
                Uri stagingUri) {
            this.request = request;
            this.destination = destination;
            this.stagingUri = stagingUri;
            machine.transition(HostStorageExportContract.ExportState.HOST_PROVIDER_SELECTED);
            directories.put("", stagingUri);
            snapshot = result(HostStorageExportContract.ExportState.HOST_PROVIDER_SELECTED,
                    HostStorageExportContract.VerificationState.NOT_VERIFIED,
                    "SAF", stagingUri, "", 0, 0, "", "", "", "");
        }

        /**
         * Writes caller-supplied entries into provider-side staging. The caller
         * owns source enumeration; this class never scans raw Linux paths.
         */
        public synchronized void writeEntries(Iterable<? extends HostStorageProvider.SourceEntry> entries)
                throws HostStorageBridge.HostStorageException {
            if (entries == null) {
                fail(HostStorageExportContract.ExportState.FAILED_WRITE,
                        "saf-source-entries-missing: source entries are required");
            }
            if (cancelled || machine.isTerminal()) {
                throw new HostStorageBridge.HostStorageException(
                        HostStorageBridge.HostStorageException.Code.INTERRUPTED,
                        "saf-export-interrupted: export session is no longer writable");
            }
            machine.transition(HostStorageExportContract.ExportState.WRITING);
            try {
                for (SourceEntry entry : entries) {
                    if (cancelled) {
                        fail(HostStorageExportContract.ExportState.FAILED_INTERRUPTED,
                                "saf-export-interrupted: export was cancelled");
                    }
                    if (entry == null) {
                        fail(HostStorageExportContract.ExportState.FAILED_WRITE,
                                "saf-entry-null: source entry is null");
                    }
                    String[] parts = validateRelativePath(entry.relativePath(), entry.directory());
                    String normalized = join(parts);
                    if (!normalizedPaths.add(normalized)) {
                        fail(HostStorageExportContract.ExportState.FAILED_NAME_CONFLICT,
                                "saf-duplicate-normalized-path: " + normalized);
                    }
                    if (entry.directory()) {
                        ensureDirectory(parts);
                    } else {
                        writeFile(parts, entry);
                    }
                }
                machine.transition(HostStorageExportContract.ExportState.FINALIZED);
                snapshot = result(HostStorageExportContract.ExportState.FINALIZED,
                        HostStorageExportContract.VerificationState.NOT_VERIFIED,
                        "SAF", stagingUri, DocumentsContract.getDocumentId(stagingUri),
                        bytesProcessed, filesProcessed, "", destination.displayName,
                        "", "");
            } catch (HostStorageBridge.HostStorageException error) {
                throw error;
            } catch (RuntimeException error) {
                fail(HostStorageExportContract.ExportState.FAILED_WRITE,
                        "saf-provider-write-runtime-error: " + error.getMessage());
            }
        }

        private Uri ensureDirectory(String[] parts) throws HostStorageBridge.HostStorageException {
            Uri parent = directories.get("");
            StringBuilder path = new StringBuilder();
            for (String part : parts) {
                if (path.length() > 0) path.append('/');
                path.append(part);
                String key = path.toString();
                Uri existing = directories.get(key);
                if (existing != null) {
                    parent = existing;
                    continue;
                }
                Uri parentDocument = parent;
                Uri conflict = findChildByName(parentDocument, part);
                if (conflict != null) {
                    throw failure(HostStorageBridge.HostStorageException.Code.NAME_CONFLICT,
                            "saf-directory-name-conflict", "Provider child already exists: " + key,
                            "Choose a new export destination or name");
                }
                Uri created = createDocument(parentDocument, Document.MIME_TYPE_DIR, part);
                directories.put(key, created);
                parent = created;
            }
            return parent;
        }

        private void writeFile(String[] parts, HostStorageProvider.SourceEntry entry)
                throws HostStorageBridge.HostStorageException {
            String fileName = parts[parts.length - 1];
            String[] parentParts = new String[parts.length - 1];
            System.arraycopy(parts, 0, parentParts, 0, parentParts.length);
            Uri parent = parentParts.length == 0 ? stagingUri : ensureDirectory(parentParts);
            if (findChildByName(parent, fileName) != null) {
                throw failure(HostStorageBridge.HostStorageException.Code.NAME_CONFLICT,
                        "saf-file-name-conflict", "Provider child already exists: " + entry.relativePath(),
                        "Choose a new export destination or name");
            }
            Uri document = createDocument(parent, entry.mimeType(), fileName);
            InputStream input = null;
            OutputStream output = null;
            try {
                input = entry.openStream();
                if (input == null) {
                    throw new IOException("source stream is null");
                }
                output = resolver.openOutputStream(document, "w");
                if (output == null) {
                    throw new IOException("provider output stream is null");
                }
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (cancelled) {
                        throw new IOException("export cancelled");
                    }
                    output.write(buffer, 0, read);
                    bytesProcessed += read;
                }
                output.flush();
                filesProcessed++;
            } catch (SecurityException error) {
                deleteQuietly(document);
                throw failure(HostStorageBridge.HostStorageException.Code.PERMISSION_DENIED,
                        "saf-file-write-permission-denied", "Android denied provider file write",
                        "Choose another Android storage folder");
            } catch (IOException error) {
                deleteQuietly(document);
                HostStorageBridge.HostStorageException.Code code = isStorageFull(error)
                        ? HostStorageBridge.HostStorageException.Code.STORAGE_FULL
                        : HostStorageBridge.HostStorageException.Code.WRITE_FAILED;
                throw failure(code, code == HostStorageBridge.HostStorageException.Code.STORAGE_FULL
                                ? "saf-storage-full" : "saf-file-write-failed",
                        error.getMessage() == null ? "Provider file write failed" : error.getMessage(),
                        "Choose another Android storage folder or retry");
            } finally {
                closeQuietly(output);
                closeQuietly(input);
            }
        }

        private Uri createDocument(Uri parent, String mimeType, String displayName)
                throws HostStorageBridge.HostStorageException {
            try {
                Uri result = DocumentsContract.createDocument(resolver, parent, mimeType, displayName);
                if (result == null) {
                    throw failure(HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                            "saf-document-create-null", "SAF provider returned no document URI",
                            "Choose another Android storage folder");
                }
                return result;
            } catch (SecurityException error) {
                throw failure(HostStorageBridge.HostStorageException.Code.PERMISSION_DENIED,
                        "saf-document-create-permission-denied", "Android denied document creation",
                        "Choose another Android storage folder");
            } catch (FileNotFoundException error) {
                throw failure(HostStorageBridge.HostStorageException.Code.PROVIDER_UNAVAILABLE,
                        "saf-document-create-provider-error", "SAF provider could not create a document",
                        "Choose another Android storage provider");
            }
        }

        @Override
        public String requestId() {
            return request.requestId();
        }

        @Override
        public HostStorageExportContract.ExportRequest request() {
            return request;
        }

        @Override
        public HostStorageExportContract.ExportResult snapshot() {
            return snapshot;
        }

        @Override
        public synchronized void cancel() {
            if (machine.isTerminal()) return;
            cancelled = true;
            try {
                machine.cancel();
                snapshot = result(HostStorageExportContract.ExportState.CANCELLED,
                        HostStorageExportContract.VerificationState.FAILED,
                        "SAF", stagingUri, DocumentsContract.getDocumentId(stagingUri),
                        bytesProcessed, filesProcessed, "CANCELLED", "", "saf-export-cancelled",
                        "Start a new export request");
            } catch (RuntimeException ignored) {
                // State is already terminal or provider cleanup is deferred.
            }
        }

        private void fail(HostStorageExportContract.ExportState state, String message)
                throws HostStorageBridge.HostStorageException {
            cancelled = true;
            machine.fail(state, message);
            snapshot = result(state, HostStorageExportContract.VerificationState.FAILED,
                    "SAF", stagingUri, DocumentsContract.getDocumentId(stagingUri),
                    bytesProcessed, filesProcessed, "", "", state.name(), message);
            throw new HostStorageBridge.HostStorageException(
                    mapFailureCode(state), message);
        }

        private HostStorageExportContract.ExportResult result(
                HostStorageExportContract.ExportState state,
                HostStorageExportContract.VerificationState verificationState,
                String provider,
                Uri uri,
                String documentId,
                long byteSize,
                long fileCount,
                String sha256,
                String destinationLabel,
                String errorCode,
                String errorMessage) {
            HostStorageExportContract.ExportResult.Builder builder =
                    HostStorageExportContract.ExportResult.builder()
                            .requestId(request.requestId())
                            .state(state)
                            .verificationState(verificationState)
                            .providerName(provider)
                            .documentId(documentId)
                            .uri(uri == null ? "" : uri.toString())
                            .displayName(request.displayName())
                            .mimeType(request.mimeType())
                            .byteSize(byteSize)
                            .fileCount(fileCount)
                            .destinationLabel(destinationLabel);
            if (sha256 != null && sha256.matches("[0-9a-fA-F]{64}")) {
                builder.sha256(sha256);
            }
            if (errorCode != null && !errorCode.isEmpty()) builder.errorCode(errorCode);
            if (errorMessage != null && !errorMessage.isEmpty()) builder.errorMessage(errorMessage);
            return builder.build();
        }

        private void deleteQuietly(Uri document) {
            try {
                DocumentsContract.deleteDocument(resolver, document);
            } catch (FileNotFoundException ignored) {
                // Best effort cleanup is limited to this request's document.
            } catch (RuntimeException ignored) {
                // Best effort cleanup is limited to this request's document.
            }
        }
    }


    private static final class ValidatedDestination {
        private final String canonicalUri;
        private final String documentId;
        private final String providerName;
        private final String displayName;

        private ValidatedDestination(
                String canonicalUri,
                String documentId,
                String providerName,
                String displayName) {
            this.canonicalUri = canonicalUri;
            this.documentId = documentId;
            this.providerName = providerName;
            this.displayName = displayName;
        }
    }

    private static String join(String[] parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) result.append('/');
            result.append(part);
        }
        return result.toString();
    }

    private static boolean isStorageFull(IOException error) {
        String message = error.getMessage();
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.US);
        return lower.contains("enospc") || lower.contains("no space") || lower.contains("space left");
    }

    private static void closeQuietly(OutputStream stream) {
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    private static HostStorageBridge.HostStorageException.Code mapFailureCode(
            HostStorageExportContract.ExportState state) {
        switch (state) {
            case FAILED_PERMISSION_DENIED:
                return HostStorageBridge.HostStorageException.Code.PERMISSION_DENIED;
            case FAILED_STORAGE_FULL:
                return HostStorageBridge.HostStorageException.Code.STORAGE_FULL;
            case FAILED_INTERRUPTED:
                return HostStorageBridge.HostStorageException.Code.INTERRUPTED;
            case FAILED_NAME_CONFLICT:
                return HostStorageBridge.HostStorageException.Code.NAME_CONFLICT;
            case FAILED_FINALIZATION:
                return HostStorageBridge.HostStorageException.Code.FINALIZATION_FAILED;
            default:
                return HostStorageBridge.HostStorageException.Code.WRITE_FAILED;
        }
    }
}
