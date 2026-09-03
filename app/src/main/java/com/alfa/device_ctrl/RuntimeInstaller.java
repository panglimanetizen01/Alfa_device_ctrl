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

    /** Preserved API; production callers must use the packaged-engine constructor. */
    public RuntimeInstaller(File vault, Progress progress) {
        this(vault, progress, null, null, RuntimeInstaller::runRuntimeSmokeTest);
    }

    /** Production constructor. packagedEngine must be nativeLibraryDir/libproot.so. */
    public RuntimeInstaller(File vault, Progress progress, File packagedEngine, File nativeLibraryDir) {
        this(vault, progress, packagedEngine, nativeLibraryDir, RuntimeInstaller::runRuntimeSmokeTest);
    }

    RuntimeInstaller(File vault, Progress progress, File packagedEngine, File nativeLibraryDir,
                     SmokeRunner smokeRunner) {
        if (vault == null || vault.getAbsolutePath().startsWith("/home/userland")) {
            throw new IllegalArgumentException("vault is outside Alfa boundary");
        }
        this.vault = vault;
        this.progress = progress;
        this.packagedEngine = packagedEngine;
        this.nativeLibraryDir = nativeLibraryDir;
        this.smokeRunner = smokeRunner == null ? RuntimeInstaller::runRuntimeSmokeTest : smokeRunner;
    }

    /**
     * Existing install signature is preserved. engineUrl is intentionally ignored:
     * PRoot is never downloaded into filesDir or runtime-vault.
     */
    public Result install(String runtimeId, URL ignoredEngineUrl, String engineSha256,
                          URL archiveUrl, String archiveSha256, boolean gzip) {
        if (!validToken(runtimeId) || archiveUrl == null || !validSha(engineSha256)
                || !validSha(archiveSha256)) {
            return Result.fail(runtimeId, "invalid-install-request", null);
        }
        File runtime = new File(vault, "runtimes/" + runtimeId);
        File staging = new File(vault, ".staging/" + runtimeId + "." + UUID.randomUUID());
        File stagedRoot = new File(staging, "rootfs");
        try {
            report("INSTALL_STAGE_CREATED");
            if (packagedEngine == null || nativeLibraryDir == null) {
                throw new IOException("packaged-engine-required");
            }
            if (!staging.mkdirs() || !stagedRoot.mkdirs()) throw new IOException("cannot-create-staging");

            // PATCH-1: PRoot must receive a real, app-private, staging-local
            // temporary workspace through the PRoot process environment.
            File prootTmp = new File(staging, "proot_tmp");
            if (!prootTmp.mkdirs()) throw new IOException("cannot-create-proot-tmp");

            File canonicalStaging = staging.getCanonicalFile();
            File canonicalProotTmp = prootTmp.getCanonicalFile();

            if (!canonicalProotTmp.toPath().startsWith(canonicalStaging.toPath())) {
                throw new IOException("proot-tmp-outside-staging");
            }
            if (!canonicalProotTmp.isDirectory()) {
                throw new IOException("proot-tmp-not-directory");
            }
            if (!canonicalProotTmp.canWrite()) {
                throw new IOException("proot-tmp-not-writable");
            }
            if (!canonicalProotTmp.canExecute()) {
                throw new IOException("proot-tmp-not-executable");
            }

            report("PROOT_TMP_DIR_READY");

            if (!TRUSTED_PROOT_ARM64_SHA256.equalsIgnoreCase(engineSha256)) {
                throw new IOException("runtime-engine-trusted-sha-mismatch");
            }
            validatePackagedEngine(packagedEngine, nativeLibraryDir);
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
        if (!TRUSTED_PROOT_ARM64_SHA256.equalsIgnoreCase(actual)) {
            throw new IOException("runtime-engine-checksum-mismatch expected=" + TRUSTED_PROOT_ARM64_SHA256 + " actual=" + actual);
        }
        report("PACKAGED_ENGINE_VERIFIED");
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
            return offset >= 20
                    && (header[0] & 0xff) == 0x7f
                    && header[1] == 'E' && header[2] == 'L' && header[3] == 'F'
                    && (header[4] & 0xff) == 2
                    && (header[5] & 0xff) == 1
                    && (header[18] & 0xff) == 0xb7
                    && (header[19] & 0xff) == 0x00;
        }
    }

    private void publishAtomically(File target, File staging, String runtimeId, File engine,
                                   String engineSha256, String archiveSha256) throws Exception {
        File parent = target.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) throw new IOException("runtime-parent-missing");
        File previous = new File(parent, target.getName() + ".previous");
        deleteRecursively(previous);
        if (target.exists()) moveDirectory(target.toPath(), previous.toPath());
        try {
            moveDirectory(staging.toPath(), target.toPath());
            File finalRoot = new File(target, "rootfs");
            writeReadyEvidence(target, runtimeId, engine, finalRoot, engineSha256, archiveSha256);
            if (!RuntimeEvidence.verify(new File(target, "READY.evidence"), runtimeId, engine, finalRoot)) {
                throw new IOException("ready-evidence-verification-failed");
            }
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
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             OutputStream output = new BufferedOutputStream(new FileOutputStream(temp))) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                digest.update(buffer, 0, count);
            }
        }
        String actual = hex(digest.digest());
        if (!expectedSha256.equalsIgnoreCase(actual)) {
            deleteRecursively(temp);
            throw new IOException("checksum-mismatch expected=" + expectedSha256 + " actual=" + actual);
        }
        moveFile(temp.toPath(), destination.toPath());
    }

    private void extractTar(File archive, File root, boolean gzip) throws Exception {
        try (InputStream raw = new BufferedInputStream(new FileInputStream(archive));
             InputStream input = gzip ? new GZIPInputStream(raw) : new XZInputStream(raw)) {
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
                    if (type == '2') {
                        try { Files.createSymbolicLink(target, Paths.get(link)); }
                        catch (FileAlreadyExistsException ignored) { }
                    } else {
                        pendingHardLinks.add(new HardLink(target, linkTarget));
                    }
                    skipExact(input, size);
                } else if (type == 0 || type == '0') {
                    Files.createDirectories(target.getParent());
                    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(target.toFile()))) {
                        copyExact(input, output, size);
                    }
                    applyMode(target.toFile(), header);
                } else {
                    skipExact(input, size);
                }
                long padding = (TAR_BLOCK - (size % TAR_BLOCK)) % TAR_BLOCK;
                skipExact(input, padding);
            }
            for (HardLink hardLink : pendingHardLinks) materializeHardLink(hardLink);
        }
    }

    private void materializeHardLink(HardLink hardLink) throws IOException {
        if (!Files.exists(hardLink.target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("hardlink-target-missing");
        Files.createDirectories(hardLink.entry.getParent());
        try {
            Files.createLink(hardLink.entry, hardLink.target);
        } catch (UnsupportedOperationException | IOException linkFailure) {
            Files.copy(hardLink.target, hardLink.entry, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private static final class HardLink {
        final Path entry;
        final Path target;
        HardLink(Path entry, Path target) { this.entry = entry; this.target = target; }
    }

    private void validateRootfs(File root) throws IOException {
        if (!root.isDirectory()) throw new IOException("rootfs-missing");
        String[] required = {"bin", "etc", "usr", "usr/bin/env"};
        for (String entry : required) if (!new File(root, entry).exists()) throw new IOException("rootfs-missing-" + entry);
        File shell = new File(root, "bin/sh");
        if (!shell.exists()) throw new IOException("rootfs-shell-missing");
    }

    private void writeResolver(File root) throws IOException {
        File resolver = new File(root, "etc/resolv.conf");
        if (resolver.exists() && !resolver.delete()) throw new IOException("resolver-replace-failed");
        File parent = resolver.getParentFile();
        if (parent == null || !parent.exists() && !parent.mkdirs()) throw new IOException("resolver-parent-missing");
        try (FileOutputStream output = new FileOutputStream(resolver)) {
            output.write("# Alfa runtime default; network capability remains evidence-dependent.\nnameserver 1.1.1.1\nnameserver 8.8.8.8\n".getBytes(StandardCharsets.UTF_8));
        }
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

            ProcessBuilder builder = new ProcessBuilder(
                    engine.getAbsolutePath(), "-0", "-r", root.getAbsolutePath(),
                    "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root",
                    "/usr/bin/env", "-i", "HOME=/root",
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                    "TERM=xterm-256color",
                    "PROOT_TMP_DIR=" + canonicalProotTmp.getAbsolutePath(),
                    "/bin/sh", "-c",
                    "printf ALFA_RUNTIME_SMOKE_OK; id -u; pwd");
            builder.environment().put("PROOT_TMP_DIR", canonicalProotTmp.getAbsolutePath());
            builder.directory(root.getParentFile());
            builder.redirectErrorStream(true);
            process = builder.start();
            if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "runtime-smoke-timeout";
            }
            String output;
            try (InputStream input = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                StringBuilder text = new StringBuilder();
                int count;
                while ((count = input.read(buffer)) != -1) text.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
                output = text.toString();
            }
            if (process.exitValue() != 0) return "runtime-smoke-exit-" + process.exitValue() + ":" + compact(output);
            if (!output.contains("ALFA_RUNTIME_SMOKE_OK")) return "runtime-smoke-marker-missing:" + compact(output);
            if (!output.matches("(?s).*\\n0\\n/.*")) return "runtime-smoke-contract-mismatch:" + compact(output);
            return null;
        } catch (Exception error) {
            if (process != null) process.destroyForcibly();
            return "runtime-smoke-exec-" + error.getClass().getSimpleName() + ":" + compact(error.getMessage());
        }
    }

    private static String compact(String value) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private void writeReadyEvidence(File staging, String runtimeId, File engine, File root,
                                    String engineSha256, String archiveSha256) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("schema_version", SCHEMA_VERSION);
        properties.setProperty("status", "READY");
        properties.setProperty("runtime_id", runtimeId);
        properties.setProperty("engine_path", engine.getCanonicalPath());
        properties.setProperty("engine_source", "ApplicationInfo.nativeLibraryDir");
        properties.setProperty("engine_abi", "arm64-v8a");
        properties.setProperty("rootfs_path", root.getCanonicalPath());
        properties.setProperty("engine_sha256", engineSha256.toLowerCase());
        properties.setProperty("archive_sha256", archiveSha256.toLowerCase());
        properties.setProperty("smoke_contract", "ALFA_RUNTIME_SMOKE_OK;uid=0;rootfs-pwd");
        properties.setProperty("created_at_epoch_ms", Long.toString(System.currentTimeMillis()));
        try (FileOutputStream output = new FileOutputStream(new File(staging, "READY.evidence"))) {
            properties.store(output, "Alfa Device Ctrl runtime-ready.v1");
        }
    }

    private Path safeEntry(Path root, String name) throws IOException {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.indexOf('\0') >= 0) throw new IOException("unsafe-archive-path");
        Path resolved = root.resolve(name).normalize();
        if (!resolved.startsWith(root.normalize())) throw new IOException("archive-traversal");
        return resolved;
    }

    private Path safeHardLink(Path root, String link) throws IOException {
        if (link == null || link.isEmpty() || link.startsWith("/") || link.indexOf('\0') >= 0) throw new IOException("unsafe-hardlink");
        Path resolved = root.resolve(link).normalize();
        if (!resolved.startsWith(root.normalize())) throw new IOException("hardlink-traversal");
        return resolved;
    }

    private Path safeLink(Path root, Path entry, String link) throws IOException {
        if (link == null || link.isEmpty() || link.indexOf('\0') >= 0) throw new IOException("unsafe-link");
        Path resolved = link.startsWith("/")
                ? root.resolve(link.substring(1)).normalize()
                : entry.getParent().resolve(link).normalize();
        if (!resolved.startsWith(root.normalize())) throw new IOException("link-traversal");
        return resolved;
    }

    private void applyMode(File file, byte[] header) {
        int mode = (int) octal(header, 100, 8);
        file.setReadable((mode & 0444) != 0, false);
        file.setWritable((mode & 0222) != 0, false);
        file.setExecutable((mode & 0111) != 0, false);
    }

    private static String field(byte[] bytes, int offset, int length) { int end = offset; while (end < offset + length && bytes[end] != 0) end++; return new String(bytes, offset, end - offset, StandardCharsets.UTF_8).trim(); }
    private static long octal(byte[] bytes, int offset, int length) { long value = 0; for (int i = offset; i < offset + length && bytes[i] != 0; i++) if (bytes[i] >= '0' && bytes[i] <= '7') value = (value << 3) + bytes[i] - '0'; return value; }
    private static boolean isZeroBlock(byte[] block) { for (byte value : block) if (value != 0) return false; return true; }
    private static int readFull(InputStream input, byte[] bytes) throws IOException { int offset = 0, count; while (offset < bytes.length && (count = input.read(bytes, offset, bytes.length - offset)) > 0) offset += count; return offset; }
    private static void copyExact(InputStream input, OutputStream output, long size) throws IOException { byte[] buffer = new byte[65536]; long left = size; while (left > 0) { int count = input.read(buffer, 0, (int) Math.min(buffer.length, left)); if (count < 0) throw new EOFException("truncated-entry"); output.write(buffer, 0, count); left -= count; } }
    private static void skipExact(InputStream input, long size) throws IOException { long left = size; while (left > 0) { long skipped = input.skip(left); if (skipped <= 0) { if (input.read() < 0) throw new EOFException("truncated-padding"); skipped = 1; } left -= skipped; } }
    private static void moveFile(Path from, Path to) throws IOException { Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
    private static void moveDirectory(Path from, Path to) throws IOException { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE); }
    private static boolean validToken(String value) { return value != null && value.matches("[A-Za-z0-9._-]{1,64}"); }
    private static boolean validSha(String value) { return value != null && value.matches("[0-9a-fA-F]{64}"); }
    private static String sha256(File file) throws Exception { MessageDigest digest = MessageDigest.getInstance("SHA-256"); try (InputStream input = new BufferedInputStream(new FileInputStream(file))) { byte[] buffer = new byte[65536]; int count; while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count); } return hex(digest.digest()); }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(bytes.length * 2); for (byte value : bytes) out.append(String.format("%02x", value & 0xff)); return out.toString(); }
    private void report(String message) { if (progress != null) progress.onMessage(message); }
    private static void deleteRecursively(File file) { if (file == null || !file.exists()) return; if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) { File[] children = file.listFiles(); if (children != null) for (File child : children) deleteRecursively(child); } file.delete(); }
}
