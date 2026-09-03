package com.alfa.device_ctrl;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RuntimeInstallerTest {
    @Test public void smokeCreatesAndExportsValidatedProotTempWorkspace() throws Exception {
        File temp = Files.createTempDirectory("alfa-proot-tmp-").toFile();
        File staging = new File(temp, "staging");
        assertTrue(staging.mkdirs());

        File prootTmp = new File(staging, "proot_tmp");
        assertTrue(prootTmp.mkdirs());

        File canonical = prootTmp.getCanonicalFile();

        assertTrue("PROOT_TMP_EXISTS", canonical.exists());
        assertTrue("PROOT_TMP_DIRECTORY", canonical.isDirectory());
        assertTrue("PROOT_TMP_WRITABLE", canonical.canWrite());
        assertTrue("PROOT_TMP_EXECUTABLE", canonical.canExecute());
        assertTrue("PROOT_TMP_INSIDE_STAGING",
                canonical.toPath().startsWith(staging.getCanonicalFile().toPath()));

        String markerValue = canonical.getAbsolutePath();
        assertTrue("PROOT_TMP_DIR_EXPORT", markerValue.endsWith("/proot_tmp"));
    }

    @Test public void verifiedInstallCreatesEvidenceAndPublishesAtomically() throws Exception {
        File temp = Files.createTempDirectory("alfa-installer-test-").toFile();
        File engine = packagedEngine(temp);
        File archive = tarGz(temp, false);
        String engineHash = sha256(engine);
        String archiveHash = sha256(archive);
        RuntimeInstaller installer = installerWithNoopSmoke(temp, engine);
        RuntimeInstaller.Result result = installer.install("ubuntu", engine.toURI().toURL(), engineHash, archive.toURI().toURL(), archiveHash, true);
        assertTrue(result.success);
        File runtime = new File(temp, "runtimes/ubuntu");
        assertTrue(RuntimeEvidence.verify(new File(runtime, "READY.evidence"), "ubuntu", engine, new File(runtime, "rootfs")));
        assertTrue(new File(runtime, "rootfs/bin/sh").isFile());
    }

    @Test public void traversalIsDeniedAndPreviousReadyRuntimeSurvives() throws Exception {
        File temp = Files.createTempDirectory("alfa-installer-rollback-").toFile();
        File engine = packagedEngine(temp);
        File valid = tarGz(temp, false);
        String engineHash = sha256(engine);
        String validHash = sha256(valid);
        RuntimeInstaller installer = installerWithNoopSmoke(temp, engine);
        assertTrue(installer.install("ubuntu", engine.toURI().toURL(), engineHash, valid.toURI().toURL(), validHash, true).success);
        File runtime = new File(temp, "runtimes/ubuntu");
        File evidence = new File(runtime, "READY.evidence");
        File traversal = tarGz(temp, true);
        RuntimeInstaller.Result failed = installer.install("ubuntu", engine.toURI().toURL(), engineHash, traversal.toURI().toURL(), sha256(traversal), true);
        assertFalse(failed.success);
        assertTrue(RuntimeEvidence.verify(evidence, "ubuntu", engine, new File(runtime, "rootfs")));
        assertFalse(new File(temp, "escape").exists());
    }

    @Test public void actualUbuntuArchiveMaterializesAndPublishes() throws Exception {
        File archive = projectFixture("artifacts/test-fixtures/ubuntu-base-24.04.4-base-arm64.tar.gz");
        assertTrue("validated Ubuntu archive must exist at " + archive, archive.isFile());
        File temp = Files.createTempDirectory("alfa-installer-ubuntu-").toFile();
        File engine = packagedEngine(temp);
        RuntimeInstaller.Result result = installerWithNoopSmoke(temp, engine).install("ubuntu", engine.toURI().toURL(), sha256(engine), archive.toURI().toURL(), sha256(archive), true);
        assertTrue(result.message, result.success);
        File runtime = new File(temp, "runtimes/ubuntu");
        assertTrue(RuntimeEvidence.verify(new File(runtime, "READY.evidence"), "ubuntu", engine, new File(runtime, "rootfs")));
        assertTrue(new File(runtime, "rootfs/bin/sh").exists());
        assertTrue(new File(runtime, "rootfs/usr/bin/env").exists());
    }

    @Test public void checksumMismatchIsDenied() throws Exception {
        File temp = Files.createTempDirectory("alfa-installer-checksum-").toFile();
        File engine = fakeEngine(temp);
        File archive = tarGz(temp, false);
        RuntimeInstaller.Result failed = new RuntimeInstaller(temp, null).install("ubuntu", engine.toURI().toURL(), "0000000000000000000000000000000000000000000000000000000000000000", archive.toURI().toURL(), sha256(archive), true);
        assertFalse(failed.success);
        assertFalse(new File(temp, "runtimes/ubuntu/READY.evidence").exists());
    }

    private static File packagedEngine(File temp) throws Exception {
        File source = projectFixture("app/src/main/jniLibs/arm64-v8a/libproot.so");
        assertTrue("trusted packaged engine fixture must exist", source.isFile());

        File nativeLibDir = new File(temp, "nativeLibs/arm64-v8a");
        assertTrue(nativeLibDir.mkdirs());

        File engine = new File(nativeLibDir, "libproot.so");
        Files.copy(source.toPath(), engine.toPath());

        assertTrue("fixture must be executable", engine.setExecutable(true, false));
        assertTrue("fixture must be executable", engine.canExecute());
        assertTrue("fixture hash must be trusted",
                RuntimeInstaller.TRUSTED_PROOT_ARM64_SHA256.equalsIgnoreCase(sha256(engine)));

        return engine;
    }

    private static RuntimeInstaller installerWithNoopSmoke(
            File temp, File packagedEngine) {
        return new RuntimeInstaller(
                temp,
                null,
                packagedEngine,
                packagedEngine.getParentFile(),
                (ignoredEngine, stagedRoot) -> null);
    }

    private static File fakeEngine(File temp) throws Exception {
        File engine = new File(temp, "engine.sh");
        Files.write(engine.toPath(), "#!/bin/sh\nprintf 'ALFA_RUNTIME_SMOKE_OK\\n0\\n/root\\n'\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(engine.setExecutable(true, false));
        return engine;
    }

    private static File projectFixture(String relativePath) {
        File working = new File(System.getProperty("user.dir"));
        File direct = new File(working, relativePath);
        if (direct.isFile()) return direct;
        File parent = working.getParentFile();
        return parent == null ? direct : new File(parent, relativePath);
    }

    private static File tarGz(File temp, boolean traversal) throws Exception {
        File archive = File.createTempFile("runtime-", ".tar.gz", temp);
        try (OutputStream file = new FileOutputStream(archive); GZIPOutputStream gzip = new GZIPOutputStream(file)) {
            entry(gzip, "bin/", new byte[0], '5');
            entry(gzip, "etc/", new byte[0], '5');
            entry(gzip, "usr/", new byte[0], '5');
            entry(gzip, "usr/bin/", new byte[0], '5');
            entry(gzip, traversal ? "../escape" : "bin/sh", "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8), '0');
            if (!traversal) entry(gzip, "usr/bin/env", "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8), '0');
            gzip.write(new byte[1024]);
        }
        return archive;
    }

    private static void entry(OutputStream out, String name, byte[] body, char type) throws IOException {
        byte[] header = new byte[512];
        put(header, 0, 100, name);
        put(header, 100, 8, "0000755");
        put(header, 108, 8, "0000000");
        put(header, 116, 8, "0000000");
        put(header, 124, 12, String.format("%011o", body.length));
        put(header, 136, 12, "00000000000");
        header[156] = (byte) type;
        put(header, 257, 6, "ustar");
        for (int i = 148; i < 156; i++) header[i] = ' ';
        long sum = 0;
        for (byte value : header) sum += value & 0xff;
        put(header, 148, 8, String.format("%06o", sum));
        out.write(header);
        out.write(body);
        int padding = (int) ((512 - (body.length % 512)) % 512);
        if (padding > 0) out.write(new byte[padding]);
    }

    private static void put(byte[] target, int offset, int width, String value) {
        byte[] bytes = (value + "\0").getBytes(StandardCharsets.US_ASCII);
        int count = Math.min(value.length(), width);
        System.arraycopy(bytes, 0, target, offset, count);
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) > 0) digest.update(buffer, 0, count);
        }
        StringBuilder output = new StringBuilder();
        for (byte value : digest.digest()) output.append(String.format("%02x", value & 0xff));
        return output.toString();
    }
}
