package com.alfa.device_ctrl;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RuntimeEvidenceAbiTest {
    @Test public void loaderTrustIsScopedToAbi() {
        assertTrue(RuntimeEvidence.isTrustedProotLoaderSha256ForAbi(
                RuntimeAbi.ARM64_V8A, RuntimeEvidence.TRUSTED_PROOT_LOADER_ARM64_SHA256));
        assertFalse(RuntimeEvidence.isTrustedProotLoaderSha256ForAbi(
                RuntimeAbi.X86_64, RuntimeEvidence.TRUSTED_PROOT_LOADER_CI_X86_64_SHA256));
        assertFalse(RuntimeEvidence.isTrustedProotLoaderSha256ForAbi(
                RuntimeAbi.X86_64, RuntimeEvidence.TRUSTED_PROOT_LOADER_ARM64_SHA256));
    }

    @Test public void x86EngineRemainsFailClosedUntilObservedArtifactIsTrusted() {
        assertFalse(RuntimeEvidence.isTrustedProotSha256ForAbi(
                RuntimeAbi.X86_64, RuntimeEvidence.TRUSTED_PROOT_ARM64_SHA256));
    }

    @Test public void elfMachineRejectsWrongAbi() throws Exception {
        File elf = Files.createTempFile("alfa-elf", ".bin").toFile();
        try {
            byte[] h = new byte[20];
            h[0] = 0x7f; h[1] = 'E'; h[2] = 'L'; h[3] = 'F';
            h[4] = 2; h[5] = 1;
            h[18] = 62; h[19] = 0;
            try (FileOutputStream out = new FileOutputStream(elf)) { out.write(h); }
            assertTrue(RuntimeEvidence.isExpectedElf(elf, RuntimeAbi.X86_64));
            assertFalse(RuntimeEvidence.isExpectedElf(elf, RuntimeAbi.ARM64_V8A));
        } finally {
            elf.delete();
        }
    }
}