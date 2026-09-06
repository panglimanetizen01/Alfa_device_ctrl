package com.alfa.device_ctrl;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provider-neutral contract for an Android host-storage implementation.
 *
 * <p>This is a boundary only. It deliberately contains no Android classes,
 * ContentResolver calls, SAF operations, MediaStore operations, file writers,
 * recursive traversal, verification, or rediscovery logic.</p>
 */
public interface HostStorageProvider {
    /** Stable provider identifier, for example a DocumentsProvider authority. */
    String providerName();

    /** Whether this provider can handle the requested destination mode. */
    boolean supports(HostStorageExportContract.DestinationMode destinationMode);

    /**
     * Starts a provider operation boundary for a previously selected destination.
     * The implementation of this interface is introduced in a later step.
     */
    ExportSession begin(
            HostStorageExportContract.ExportRequest request,
            ProviderDestination destination) throws HostStorageBridge.HostStorageException;

    /**
     * Provider-neutral destination metadata. A provider adapter maps its Android
     * URI/document representation into this boundary; this type performs no I/O.
     */
    interface ProviderDestination {
        String providerName();

        String canonicalUri();

        String documentId();

        String displayName();
    }

    /**
     * Provider-neutral source entry supplied by the caller. The provider owns
     * the destination write; it never receives a raw source filesystem path.
     */
    interface SourceEntry {
        String relativePath();

        boolean directory();

        String mimeType();

        InputStream openStream() throws IOException;
    }

    /**
     * Provider operation lifecycle boundary. Later steps may add verification
     * behind this interface without changing provider callers.
     */
    interface ExportSession {
        String requestId();

        HostStorageExportContract.ExportRequest request();

        HostStorageExportContract.ExportResult snapshot();

        void writeEntries(Iterable<? extends SourceEntry> entries)
                throws HostStorageBridge.HostStorageException;

        void cancel();
    }
}
