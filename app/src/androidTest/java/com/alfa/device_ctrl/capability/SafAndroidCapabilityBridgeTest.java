package com.alfa.device_ctrl.capability;

import android.net.Uri;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class SafAndroidCapabilityBridgeTest {

    @Test
    public void contractUsesRequiredCapabilityAndPayload() {
        assertEquals(
                "storage.saf.roundtrip",
                SafAndroidCapabilityBridge.CAPABILITY);

        assertEquals(
                "ALFA_SAF_PROBE_V1",
                SafAndroidCapabilityBridge.PAYLOAD);
    }

    @Test
    public void payloadDigestIsCorrect() {
        assertEquals(
                "5f944dd62a78fa09b8885be1d1670d2d4a5668befb83b9b5666e2efcb9a2b90a",
                SafAndroidCapabilityBridge.sha256(
                        SafAndroidCapabilityBridge.PAYLOAD));
    }

    @Test
    public void allStatusesAreExplicit() {
        assertEquals(
                SafAndroidCapabilityBridge.Status.PASS,
                SafAndroidCapabilityBridge.Result
                        .pass(null, 1, 1, "digest", true, true)
                        .getStatus());

        assertEquals(
                SafAndroidCapabilityBridge.Status.DENIED,
                SafAndroidCapabilityBridge.Result
                        .failure(
                                SafAndroidCapabilityBridge.Status.DENIED,
                                "denied")
                        .getStatus());

        assertEquals(
                SafAndroidCapabilityBridge.Status.UNSUPPORTED,
                SafAndroidCapabilityBridge.Result
                        .failure(
                                SafAndroidCapabilityBridge.Status.UNSUPPORTED,
                                "unsupported")
                        .getStatus());

        assertEquals(
                SafAndroidCapabilityBridge.Status.ERROR,
                SafAndroidCapabilityBridge.Result
                        .failure(
                                SafAndroidCapabilityBridge.Status.ERROR,
                                "error")
                        .getStatus());
    }

    @Test
    public void passResultPreservesCountsAndDigest() {
        SafAndroidCapabilityBridge.Result result =
                SafAndroidCapabilityBridge.Result.pass(
                        null,
                        17,
                        17,
                        SafAndroidCapabilityBridge.sha256(
                                "ALFA_SAF_PROBE_V1"),
                        true,
                        true);

        assertEquals(17, result.getBytesWritten());
        assertEquals(17, result.getBytesRead());

        assertEquals(
                "5f944dd62a78fa09b8885be1d1670d2d4a5668befb83b9b5666e2efcb9a2b90a",
                result.getContentSha256());

        assertTrue(result.hasPersistedReadPermission());
        assertTrue(result.hasPersistedWritePermission());
        assertTrue(result.isMetadataVerified());
    }

    @Test
    public void evidenceRedactsActualUriAndKeepsHandle() {
        final String uriString =
                "content://provider/private/location";

        Uri uri = Uri.parse(uriString);

        SafAndroidCapabilityBridge.Result result =
                SafAndroidCapabilityBridge.Result.pass(
                        uri, 17, 17, "digest", true, true);

        String evidence = result.toEvidenceJson();

        assertNotNull(result.getCapabilityHandle());

        assertFalse(evidence.contains(uriString));

        assertTrue(
                evidence.contains("capabilityHandleHash"));

        assertTrue(
                evidence.contains(
                        SafAndroidCapabilityBridge.sha256(
                                uriString)));
    }
}
