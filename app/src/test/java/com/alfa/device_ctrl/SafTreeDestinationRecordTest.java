package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SafTreeDestinationRecordTest {
    @Test public void recordRequiresCanonicalContentUriAndPreservesProviderMetadata() {
        SafTreeDestinationRecord record = new SafTreeDestinationRecord(
                "req-3",
                "content://com.android.providers.media.documents/tree/primary%3ADownload",
                "com.android.providers.media.documents",
                "primary:Download",
                "Download",
                3,
                HostStorageExportContract.VerificationState.NOT_VERIFIED,
                123L);

        assertEquals("req-3", record.requestId());
        assertEquals("content://com.android.providers.media.documents/tree/primary%3ADownload", record.treeUri());
        assertEquals("com.android.providers.media.documents", record.providerAuthority());
        assertEquals("primary:Download", record.documentId());
        assertEquals(3, record.persistedPermissionFlags());
        assertEquals(HostStorageExportContract.VerificationState.NOT_VERIFIED, record.verificationState());
        assertTrue(record.recordedAtEpochMillis() == 123L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rawFilesystemPathIsRejectedAsDestination() {
        new SafTreeDestinationRecord(
                "req-3",
                "/storage/emulated/0/Download",
                "com.android.providers.media.documents",
                "primary:Download",
                "Download",
                3,
                HostStorageExportContract.VerificationState.NOT_VERIFIED,
                123L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mismatchedAuthorityIsRejected() {
        new SafTreeDestinationRecord(
                "req-3",
                "content://com.android.providers.media.documents/tree/primary%3ADownload",
                "wrong.provider",
                "primary:Download",
                "Download",
                3,
                HostStorageExportContract.VerificationState.NOT_VERIFIED,
                123L);
    }
}
