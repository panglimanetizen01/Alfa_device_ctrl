package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Properties;

import org.junit.Test;

public final class DebianRuntimeInstallerTest {
    private static final String DEBIAN_URL = RuntimeProfile.ROOTFS_URL;
    private static final String DEBIAN_SHA256 = RuntimeProfile.ROOTFS_SHA256;

    @Test public void canonicalDebianArtifactInstallsAndProducesVerifiedReadyEvidence() throws Exception {
        File archive = fixture("artifacts/test-fixtures/debian-bookworm-arm64.tar.gz");
        if (!archive.isFile()) archive = downloadPinnedDebianArtifact();
        assertTrue("Debian acceptance fixture missing: " + archive, archive.isFile());
        File temp = Files.createTempDirectory("alfa-debian-installer-").toFile();
        File nativeDir = new File(temp, "nativeLibs/arm64-v8a");
        assertTrue(nativeDir.mkdirs());
        File engineSource = fixture("app/src/main/jniLibs/arm64-v8a/libproot.so");
        File loaderSource = fixture("app/build/generated/jniLibs/arm64-v8a/libproot-loader.so");
        File engine = new File(nativeDir, "libproot.so");
        File loader = new File(nativeDir, "libproot-loader.so");
        Files.copy(engineSource.toPath(), engine.toPath());
        Files.copy(loaderSource.toPath(), loader.toPath());
        assertTrue(engine.setExecutable(true, false));
        assertTrue(loader.setExecutable(true, false));
        assertEquals(RuntimeInstaller.TRUSTED_PROOT_ARM64_SHA256, sha256(engine));
        assertEquals(RuntimeEvidence.TRUSTED_PROOT_LOADER_ARM64_SHA256, sha256(loader));

        RuntimeInstaller installer = new RuntimeInstaller(temp, null, engine, nativeDir, (ignored, root) -> null);
        RuntimeInstaller.Result result = installer.install(RuntimeProfile.ID, null, sha256(engine), archive.toURI().toURL(), sha256(archive), true);
        assertTrue(result.message, result.success);

        File runtime = new File(temp, "runtimes/" + RuntimeProfile.ID);
        File evidence = new File(runtime, "READY.evidence");
        File rootfs = new File(runtime, "rootfs");
        assertTrue(RuntimeEvidence.verify(evidence, RuntimeProfile.ID, engine, rootfs));
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(new File(rootfs, "etc/os-release"))) { p.load(input); }
        assertEquals("debian", p.getProperty("ID"));
        assertTrue(new File(rootfs, "bin/sh").exists());
        assertTrue(new File(rootfs, "usr/bin/env").exists());
    }

    private static File fixture(String relative) {
        File direct = new File(System.getProperty("user.dir"), relative);
        if (direct.isFile()) return direct;
        return new File(new File(System.getProperty("user.dir")).getParentFile(), relative);
    }

    private static File downloadPinnedDebianArtifact() throws Exception {
        File archive = Files.createTempFile("alfa-debian-bookworm-", ".tar.gz").toFile();
        archive.deleteOnExit();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(DEBIAN_URL)).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(archive.toPath()));
        assertEquals("Debian rootfs download failed", 200, response.statusCode());
        assertEquals("Debian rootfs digest mismatch", DEBIAN_SHA256, sha256(archive));
        return archive;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) > 0) digest.update(buffer, 0, count);
        }
        StringBuilder out = new StringBuilder(64);
        for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }
}
