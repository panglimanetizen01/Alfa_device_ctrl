package com.alfa.device_ctrl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.XZInputStream;

/**
 * Installs one verified ARM64 rootless runtime. PRoot is read-only packaged code
 * from ApplicationInfo.nativeLibraryDir; only the rootfs and evidence are writable.
 */
public final class RuntimeInstaller {
    public static final String SCHEMA_VERSION = "runtime-ready.v1";
    public static final String TRUSTED_PROOT_ARM64_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    public static final String TRUSTED_PROOT_LOADER_ARM64_SHA256 = "b165c63ef14d274ddc7bc83e1e624fbb566d8cbd4a95a1d1891c7c6d8fd04baa";
    private static final int TAR_BLOCK = 512;

    public interface Progress { void onMessage(String message); }
    interface SmokeRunner { String run(File engine, File root); }

    public static final class Result {
        public final boolean success;
        public final String runtimeId;
        public final String message;
        public final File runtimeDirectory;
        private Result(boolean success, String runtimeId, String message, File runtimeDirectory) {
            this.success = success;
            this.runtimeId = runtimeId;
            this.message = message;
            this.runtimeDirectory = runtimeDirectory;
        }
        public static Result ok(String id, File dir) { return new Result(true, id, "READY", dir); }
        public static Result fail(String id, String message, File dir) { return new Result(false, id, message, dir); }
    }

    private final File vault;
    private final Progress progress;
    private final File packagedEngine;
    private final File nativeLibraryDir;
    private final SmokeRunner smokeRunner;

    public RuntimeInstaller(File vault, Progress progress) {
        this(vault, progress, null, null, RuntimeInstaller::runRuntimeSmokeTest);
    }

    public RuntimeInstaller(File vault, Progress progress, File packagedEngine, File nativeLibraryDir) {
        this(vault, progress, packagedEngine, nativeLibraryDir, RuntimeInstaller::runRuntimeSmokeTest);
    }

    RuntimeInstaller(File vault, Progress progress, File packagedEngine, File nativeLibraryDir, SmokeRunner smokeRunner) {
        if (vault == null || vault.getAbsolutePath().startsWith("/home/userland")) throw new IllegalArgumentException("vault is outside Alfa boundary");
        this.vault = vault;
        this.progress = progress;
        this.packagedEngine = packagedEngine;
        this.nativeLibraryDir = nativeLibraryDir;
        this.smokeRunner = smokeRunner == null ? RuntimeInstaller::runRuntimeSmokeTest : smokeRunner;
    }

    public Result install(String runtimeId, URL ignoredEngineUrl, String engineSha256, URL archiveUrl, String archiveSha256, boolean gzip) {
        if (!validToken(runtimeId) || archiveUrl == null || !validSha(engineSha256) || !validSha(archiveSha256)) return Result.fail(runtimeId, "invalid-install-request", null);
        File runtime = new File(vault, "runtimes/" + runtimeId);
        File staging = new File(vault, ".staging/" + runtimeId + "." + UUID.randomUUID());
        File stagedRoot = new File(staging, "rootfs");
        try {
            report("INSTALL_STAGE_CREATED");
            if (packagedEngine == null || nativeLibraryDir == null) throw new IOException("packaged-engine-required");
            if (!staging.mkdirs() || !stagedRoot.mkdirs()) throw new IOException("cannot-create-staging");
            File prootTmp = new File(staging, "proot_tmp");
            if (!prootTmp.mkdirs()) throw new IOException("cannot-create-proot-tmp");
            File canonicalStaging = staging.getCanonicalFile();
            File canonicalProotTmp = prootTmp.getCanonicalFile();
            if (!canonicalProotTmp.toPath().startsWith(canonicalStaging.toPath())) throw new IOException("proot-tmp-outside-staging");
            if (!canonicalProotTmp.isDirectory()) throw new IOException("proot-tmp-not-directory");
            if (!canonicalProotTmp.canWrite()) throw new IOException("proot-tmp-not-writable");
            if (!canonicalProotTmp.canExecute()) throw new IOException("proot-tmp-not-executable");
            report("PROOT_TMP_DIR_READY");
            if (!TRUSTED_PROOT_ARM64_SHA256.equalsIgnoreCase(engineSha256)) throw new IOException("runtime-engine-trusted-sha-mismatch");
            validatePackagedEngine(packagedEngine, nativeLibraryDir);
            validatePackagedLoader(packagedEngine, nativeLibraryDir);
            File archive = new File(staging, "rootfs.archive");
            downloadVerified(archiveUrl, archive, archiveSha256);
            extractTar(archive, stagedRoot, gzip);
            validateRootfs(stagedRoot);
            writeResolver(stagedRoot);
            String smokeFailure = smokeRunner.run(packagedEngine, stagedRoot);
            if (smokeFailure != null) throw new IOException(smokeFailure);
            publishAtomically(runtime, staging, runtimeId, packagedEngine, engineSha256, archiveSha256);
            deleteRecursively(new File(runtime, "rootfs.archive"));
            report("INSTALL_COMMITTED");
            return Result.ok(runtimeId, runtime);
        } catch (Exception error) {
            report("INSTALL_FAILED " + error.getClass().getSimpleName());
            deleteRecursively(staging);
            return Result.fail(runtimeId, error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage()), runtime);
        }
    }

    private void validatePackagedEngine(File engine, File libraryDir) throws Exception {
        if (!engine.isFile()) throw new IOException("runtime-engine-missing");
        if (!engine.getName().equals("libproot.so")) throw new IOException("runtime-engine-name-invalid");
        File canonicalEngine = engine.getCanonicalFile();
        File canonicalDir = libraryDir.getCanonicalFile();
        File parent = canonicalEngine.getParentFile();
        if (parent == null || !canonicalDir.equals(parent)) throw new IOException("runtime-engine-not-nativeLibraryDir");
        if (!canonicalEngine.canExecute()) throw new IOException("runtime-engine-not-executable");
        if (!isArm64Elf(canonicalEngine)) throw new IOException("runtime-engine-not-arm64-elf");
        String actual = sha256(canonicalEngine);
        if (!TRUSTED_PROOT_ARM64_SHA256.equalsIgnoreCase(actual)) throw new IOException("runtime-engine-checksum-mismatch expected=" + TRUSTED_PROOT_ARM64_SHA256 + " actual=" + actual);
        report("PACKAGED_ENGINE_VERIFIED");
    }

    private void validatePackagedLoader(File engine, File libraryDir) throws Exception {
        File canonicalDir = libraryDir.getCanonicalFile();
        File loader = new File(canonicalDir, "libproot-loader.so").getCanonicalFile();
        if (!loader.isFile()) throw new IOException("runtime-loader-missing");
        if (!loader.canExecute()) throw new IOException("runtime-loader-not-executable");
        if (!isArm64Elf(loader)) throw new IOException("runtime-loader-not-arm64-elf");
        String actual = sha256(loader);
        if (!TRUSTED_PROOT_LOADER_ARM64_SHA256.equalsIgnoreCase(actual)) throw new IOException("runtime-loader-checksum-mismatch expected=" + TRUSTED_PROOT_LOADER_ARM64_SHA256 + " actual=" + actual);
        report("PACKAGED_LOADER_VERIFIED");
    }

    private static boolean isArm64Elf(File engine) throws IOException {
        byte[] header = new byte[20];
        try (InputStream input = new FileInputStream(engine)) {
            int offset = 0;
            while (offset < header.length) {
                int count = input.read(header, offset, header.length - offset);
                if (count < 0) break;
                offset += count;
            }
            return offset >= 20 && (header[0] & 0xff) == 0x7f && header[1] == 'E' && header[2] == 'L' && header[3] == 'F' && (header[4] & 0xff) == 2 && (header[5] & 0xff) == 1 && (header[18] & 0xff) == 0xb7 && (header[19] & 0xff) == 0x00;
        }
    }

    private void publishAtomically(File target, File staging, String runtimeId, File engine, String engineSha256, String archiveSha256) throws Exception {
        File parent = target.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) throw new IOException("runtime-parent-missing");
        File previous = new File(parent, target.getName() + ".previous");
        deleteRecursively(previous);
        if (target.exists()) moveDirectory(target.toPath(), previous.toPath());
        try {
            moveDirectory(staging.toPath(), target.toPath());
            File finalRoot = new File(target, "rootfs");
            writeReadyEvidence(target, runtimeId, engine, finalRoot, engineSha256, archiveSha256);
            if (!RuntimeEvidence.verify(new File(target, "READY.evidence"), runtimeId, engine, finalRoot)) throw new IOException("ready-evidence-verification-failed");
            deleteRecursively(previous);
        } catch (Exception publishFailure) {
            deleteRecursively(target);
            if (previous.exists()) moveDirectory(previous.toPath(), target.toPath());
            throw publishFailure;
        }
    }

    private void downloadVerified(URL url, File destination, String expectedSha256) throws Exception {
        File temp = new File(destination.getParentFile(), destination.getName() + ".part");
        deleteRecursively(temp);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);
        if (connection instanceof HttpURLConnection) {
            int code = ((HttpURLConnection) connection).getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP-" + code);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new BufferedInputStream(connection.getInputStream()); OutputStream output = new BufferedOutputStream(new FileOutputStream(temp))) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) { output.write(buffer, 0, count); digest.update(buffer, 0, count); }
        }
        String actual = hex(digest.digest());
        if (!expectedSha256.equalsIgnoreCase(actual)) { deleteRecursively(temp); throw new IOException("checksum-mismatch expected=" + expectedSha256 + " actual=" + actual); }
        moveFile(temp.toPath(), destination.toPath());
    }

    private void extractTar(File archive, File root, boolean gzip) throws Exception {
        try (InputStream raw = new BufferedInputStream(new FileInputStream(archive)); InputStream input = gzip ? new GZIPInputStream(raw) : new XZInputStream(raw)) {
            List<HardLink> pendingHardLinks = new ArrayList<>();
            byte[] header = new byte[TAR_BLOCK];
            while (true) {
                int count = readFull(input, header);
                if (count == 0) break;
                if (count != TAR_BLOCK) throw new EOFException("truncated-tar-header");
                if (isZeroBlock(header)) break;
                String name = field(header, 0, 100);
                long size = octal(header, 124, 12);
                int type = header[156] & 0xff;
                String link = field(header, 157, 100);
                Path target = safeEntry(root.toPath(), name);
                if (type == '5') {
                    Files.createDirectories(target);
                } else if (type == '2' || type == '1') {
                    Path linkTarget = type == '1' ? safeHardLink(root.toPath(), link) : safeLink(root.toPath(), target, link);
                    Files.createDirectories(target.getParent());
                    if (type == '2') { try { Files.createSymbolicLink(target, Paths.get(link)); } catch (FileAlreadyExistsException ignored) { } }
                    else pendingHardLinks.add(new HardLink(target, linkTarget));
                    skipExact(input, size);
                } else if (type == 0 || type == '0') {
                    Files.createDirectories(target.getParent());
                    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(target.toFile()))) { copyExact(input, output, size); }
                    applyMode(target.toFile(), header);
                } else skipExact(input, size);
                long padding = (TAR_BLOCK - (size % TAR_BLOCK)) % TAR_BLOCK;
                skipExact(input, padding);
            }
            for (HardLink hardLink : pendingHardLinks) materializeHardLink(hardLink);
        }
    }

    private void materializeHardLink(HardLink hardLink) throws IOException {
        if (!Files.exists(hardLink.target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("hardlink-target-missing");
        Files.createDirectories(hardLink.entry.getParent());
        try { Files.createLink(hardLink.entry, hardLink.target); } catch (UnsupportedOperationException | IOException linkFailure) { Files.copy(hardLink.target, hardLink.entry, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES); }
    }

    private static final class HardLink { final Path entry; final Path target; HardLink(Path entry, Path target) { this.entry = entry; this.target = target; } }

    private void validateRootfs(File root) throws IOException {
        if (!root.isDirectory()) throw new IOException("rootfs-missing");
        String[] required = {"bin", "etc", "usr", "usr/bin/env"};
        for (String entry : required) if (!new File(root, entry).exists()) throw new IOException("rootfs-missing-" + entry);
        if (!new File(root, "bin/sh").exists()) throw new IOException("rootfs-shell-missing");
    }

    private void writeResolver(File root) throws IOException {
        File resolver = new File(root, "etc/resolv.conf");
        if (resolver.exists() && !resolver.delete()) throw new IOException("resolver-replace-failed");
        File parent = resolver.getParentFile();
        if (parent == null || !parent.exists() && !parent.mkdirs()) throw new IOException("resolver-parent-missing");
        try (FileOutputStream output = new FileOutputStream(resolver)) { output.write("# Alfa runtime default; network capability remains evidence-dependent.\nnameserver 1.1.1.1\nnameserver 8.8.8.8\n".getBytes(StandardCharsets.UTF_8)); }
    }

    private static String runRuntimeSmokeTest(File engine, File root) {
        Process process = null;
        try {
            if (!engine.isFile()) return "runtime-smoke-engine-missing";
            if (!engine.canExecute()) return "runtime-smoke-engine-not-executable";
            if (!root.isDirectory()) return "runtime-smoke-rootfs-missing";
            File prootTmp = new File(root.getParentFile(), "proot_tmp");
            File canonicalProotTmp = prootTmp.getCanonicalFile();
            if (!canonicalProotTmp.isDirectory()) return "runtime-smoke-proot-tmp-missing";
            if (!canonicalProotTmp.canWrite()) return "runtime-smoke-proot-tmp-not-writable";
            if (!canonicalProotTmp.canExecute()) return "runtime-smoke-proot-tmp-not-executable";
            File loader = new File(engine.getParentFile(), "libproot-loader.so").getCanonicalFile();
            if (!loader.isFile()) return "runtime-smoke-loader-missing";
            if (!loader.canExecute()) return "runtime-smoke-loader-not-executable";
            if (!isArm64Elf(loader)) return "runtime-smoke-loader-not-arm64-elf";
            if (!TRUSTED_PROOT_LOADER_ARM64_SHA256.equalsIgnoreCase(sha256(loader))) return "runtime-smoke-loader-checksum-mismatch";

            ProcessBuilder builder = new ProcessBuilder(engine.getAbsolutePath(), "-0", "-r", root.getAbsolutePath(), "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root", "/usr/bin/env", "-i", "HOME=/root", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "TERM=xterm-256color", "/bin/sh", "-c", "printf 'ALFA_RUNTIME_SMOKE_OK\\n'; id; uname -m");
            builder.redirectErrorStream(true);
            builder.environment().put("PROOT_TMP_DIR", canonicalProotTmp.getAbsolutePath());
            process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); return "runtime-smoke-timeout"; }
            if (process.exitValue() != 0) return "runtime-smoke-exit-" + process.exitValue() + ":" + output.trim();
            if (!output.contains("ALFA_RUNTIME_SMOKE_OK")) return "runtime-smoke-marker-missing";
            return null;
        } catch (Exception error) {
            if (process != null) process.destroyForcibly();
            return "runtime-smoke-exception:" + error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage());
        }
    }

    private void writeReadyEvidence(File runtime, String runtimeId, File engine, File root, String engineSha256, String archiveSha256) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("schema", SCHEMA_VERSION);
        properties.setProperty("status", "READY");
        properties.setProperty("runtime_id", runtimeId);
        properties.setProperty("engine_path", engine.getCanonicalPath());
        properties.setProperty("rootfs_path", root.getCanonicalPath());
        properties.setProperty("engine_sha256", engineSha256);
        properties.setProperty("archive_sha256", archiveSha256);
        properties.setProperty("loader_sha256", TRUSTED_PROOT_LOADER_ARM64_SHA256);
        properties.setProperty("engine_executable", String.valueOf(engine.canExecute()));
        properties.setProperty("rootfs_directory", String.valueOf(root.isDirectory()));
        try (OutputStream output = new FileOutputStream(new File(runtime, "READY.evidence"))) { properties.store(output, "Alfa runtime evidence"); }
    }

    private void report(String message) { if (progress != null) progress.onMessage(message); }
    private static boolean validToken(String value) { return value != null && value.matches("[A-Za-z0-9._-]{1,64}"); }
    private static boolean validSha(String value) { return value != null && value.matches("[0-9a-fA-F]{64}"); }
    private static String sha256(File file) throws Exception { MessageDigest digest = MessageDigest.getInstance("SHA-256"); try (InputStream input = new FileInputStream(file)) { byte[] buffer = new byte[65536]; int count; while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count); } return hex(digest.digest()); }
    private static String hex(byte[] bytes) { StringBuilder builder = new StringBuilder(bytes.length * 2); for (byte value : bytes) builder.append(String.format("%02x", value & 0xff)); return builder.toString(); }
    private static int readFull(InputStream input, byte[] buffer) throws IOException { int offset = 0; while (offset < buffer.length) { int count = input.read(buffer, offset, buffer.length - offset); if (count < 0) break; offset += count; } return offset; }
    private static boolean isZeroBlock(byte[] buffer) { for (byte value : buffer) if (value != 0) return false; return true; }
    private static String field(byte[] buffer, int offset, int length) { int end = offset; int limit = Math.min(buffer.length, offset + length); while (end < limit && buffer[end] != 0) end++; return new String(buffer, offset, end - offset, StandardCharsets.UTF_8); }
    private static long octal(byte[] buffer, int offset, int length) throws IOException { String value = field(buffer, offset, length).trim(); if (value.isEmpty()) return 0; try { return Long.parseLong(value, 8); } catch (NumberFormatException error) { throw new IOException("invalid-tar-size"); } }
    private static Path safeEntry(Path root, String name) throws IOException { Path target = root.resolve(name).normalize(); if (!target.startsWith(root)) throw new IOException("unsafe-tar-entry"); return target; }
    private static Path safeLink(Path root, Path entry, String link) throws IOException { Path target = entry.getParent().resolve(link).normalize(); if (!target.startsWith(root)) throw new IOException("unsafe-symlink"); return target; }
    private static Path safeHardLink(Path root, String link) throws IOException { return safeEntry(root, link); }
    private static void copyExact(InputStream input, OutputStream output, long size) throws IOException { byte[] buffer = new byte[65536]; long remaining = size; while (remaining > 0) { int count = input.read(buffer, 0, (int)Math.min(buffer.length, remaining)); if (count < 0) throw new EOFException("truncated-tar-entry"); output.write(buffer, 0, count); remaining -= count; } }
    private static void skipExact(InputStream input, long size) throws IOException { byte[] buffer = new byte[8192]; long remaining = size; while (remaining > 0) { int count = input.read(buffer, 0, (int)Math.min(buffer.length, remaining)); if (count < 0) throw new EOFException("truncated-tar-entry"); remaining -= count; } }
    private static void applyMode(File file, byte[] header) { String value = field(header, 100, 8).trim(); try { int mode = Integer.parseInt(value, 8) & 0777; if (mode != 0) file.setExecutable((mode & 0111) != 0, false); if ((mode & 0222) != 0) file.setWritable(true, false); } catch (NumberFormatException ignored) { } }
    private static void moveDirectory(Path source, Path target) throws IOException { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    private static void moveFile(Path source, Path target) throws IOException { try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); } catch (UnsupportedOperationException error) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); } }
    private static void deleteRecursively(File file) { if (!file.exists()) return; File[] children = file.listFiles(); if (children != null) for (File child : children) deleteRecursively(child); file.delete(); }
}
