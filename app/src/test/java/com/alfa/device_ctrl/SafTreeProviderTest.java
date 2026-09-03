package com.alfa.device_ctrl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SafTreeProviderTest {
    @Test public void providerImplementsProviderBoundary() {
        assertTrue(HostStorageProvider.class.isAssignableFrom(SafTreeProvider.class));
        assertTrue(Modifier.isPublic(SafTreeProvider.class.getModifiers()));
    }

    @Test public void validRelativePathIsNormalizedToSegments() throws Exception {
        assertArrayEquals(
                new String[]{"src", "main", "AndroidManifest.xml"},
                SafTreeProvider.validateRelativePath(
                        "src/main/AndroidManifest.xml", false));
        assertArrayEquals(
                new String[]{"docs", "nested"},
                SafTreeProvider.validateRelativePath("docs\\nested", true));
    }

    @Test
    public void spacesAndUnicodeFilenameSegmentsAreAccepted() throws Exception {
        assertArrayEquals(
                new String[]{"proof-root", "nested", "space file.txt"},
                SafTreeProvider.validateRelativePath(
                        "proof-root/nested/space file.txt", false));

        assertArrayEquals(
                new String[]{"proof-root", "nested", "Unicode-文件.txt"},
                SafTreeProvider.validateRelativePath(
                        "proof-root/nested/Unicode-文件.txt", false));
    }

    @Test(expected = HostStorageBridge.HostStorageException.class)
    public void controlCharacterInFilenameSegmentIsRejected() throws Exception {
        SafTreeProvider.validateRelativePath(
                "proof-root/nested/bad\u0007name.txt", false);
    }

    @Test(expected = HostStorageBridge.HostStorageException.class)
    public void absolutePathIsRejected() throws Exception {
        SafTreeProvider.validateRelativePath("/storage/emulated/0/file", false);
    }

    @Test(expected = HostStorageBridge.HostStorageException.class)
    public void parentTraversalIsRejected() throws Exception {
        SafTreeProvider.validateRelativePath("docs/../secret.txt", false);
    }

    @Test(expected = HostStorageBridge.HostStorageException.class)
    public void nulPathIsRejected() throws Exception {
        SafTreeProvider.validateRelativePath("docs/ok\0.txt", false);
    }

    @Test(expected = HostStorageBridge.HostStorageException.class)
    public void duplicateSeparatorsAreRejected() throws Exception {
        SafTreeProvider.validateRelativePath("docs//file.txt", false);
    }

    @Test public void stagingNameIsProviderSideAndRequestBound() throws Exception {
        assertEquals(
                ".alfa-export-staging-request-01",
                SafTreeProvider.stagingName("request-01"));
    }

    @Test(expected = HostStorageBridge.HostStorageException.class)
    public void unsafeRequestIdIsRejectedBeforeProviderAccess() throws Exception {
        SafTreeProvider.stagingName("../outside");
    }

    @Test public void sourceEntryBoundaryDoesNotExposeRawFilesystemWriter() throws Exception {
        Method openStream = HostStorageProvider.SourceEntry.class.getMethod("openStream");
        assertEquals(InputStream.class, openStream.getReturnType());
        assertTrue(Arrays.stream(HostStorageProvider.SourceEntry.class.getMethods())
                .noneMatch(method -> method.getName().equals("path")
                        || method.getName().equals("file")
                        || method.getName().equals("absolutePath")));
    }

    @Test public void providerWriteFailureCodeIsStructured() {
        HostStorageBridge.HostStorageException failure =
                new HostStorageBridge.HostStorageException(
                        HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                        "saf-file-write-failed: test failure");
        assertEquals(
                HostStorageBridge.HostStorageException.Code.WRITE_FAILED,
                failure.code());
        assertTrue(failure.getMessage().startsWith("saf-file-write-failed:"));
    }

    private static final class InMemoryEntry implements HostStorageProvider.SourceEntry {
        @Override public String relativePath() { return "docs/example.txt"; }
        @Override public boolean directory() { return false; }
        @Override public String mimeType() { return "text/plain"; }
        @Override public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(new byte[]{1, 2, 3});
        }
    }
}
