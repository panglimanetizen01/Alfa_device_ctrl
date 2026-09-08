package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Process;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** Real G17 verifier. Runtime execution is performed from the instrumented app process. */
public final class G17AndroidRuntimeExecutionTest {
    private static final String PACKAGE = "com.alfa.device_ctrl";
    private static final String EXPECTED_PROOT_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    @Test
    public void exactGate16AuthorizationExecutesPwdInsidePackagedRuntime() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals(PACKAGE, context.getPackageName());
        ApplicationInfo info = context.getApplicationInfo();
        assertTrue("nativeLibraryDir missing", info.nativeLibraryDir != null);
        File engine = new File(info.nativeLibraryDir, "libproot.so");
        assertTrue("packaged PRoot missing: " + engine, engine.isFile() && engine.canExecute());
        assertEquals("packaged PRoot SHA mismatch", EXPECTED_PROOT_SHA256, sha256(engine));

        int uid = Process.myUid();
        int packageUid = context.getPackageManager().getApplicationInfo(PACKAGE, 0).uid;
        assertEquals("instrumented process is not package UID", packageUid, uid);
        assertTrue("shell UID must never be accepted", uid != 2000);
        String selinux = readFirstLine(new File("/proc/self/attr/current"));
        assertTrue("SELinux context missing", !selinux.isEmpty());
        assertTrue("shell SELinux context must never be accepted", !"u:r:shell:s0".equals(selinux));

        File runtime = new File(new File(context.getFilesDir(), "runtime-vault"), "runtimes/ubuntu");
        File ready = new File(runtime, "READY.evidence");
        File rootfs = new File(runtime, "rootfs");
        assertTrue("runtime READY evidence missing: " + ready, ready.isFile());
        assertTrue("runtime rootfs missing: " + rootfs, rootfs.isDirectory());
        File proc = new File(rootfs, "proc");
        File dev = new File(rootfs, "dev");
        File sys = new File(rootfs, "sys");
        assertTrue("rootfs /proc missing", proc.isDirectory() || proc.mkdirs());
        assertTrue("rootfs /dev missing", dev.isDirectory() || dev.mkdirs());
        assertTrue("rootfs /sys missing", sys.isDirectory() || sys.mkdirs());
        assertTrue("runtime evidence invalid", RuntimeEvidence.verify(ready, "ubuntu", engine, rootfs));

        String gate16Path = arg("gate16_path");
        String expectedSource = requiredArg("source_commit");
        File gate16 = new File(gate16Path);
        assertTrue("Gate16 artifact missing: " + gate16, gate16.isFile());
        Properties a = load(gate16);
        assertEquals("gate16-runtime-execution-authorization.v1", a.getProperty("schema_version"));
        assertEquals("gate16", a.getProperty("gate"));
        assertEquals("PASS", a.getProperty("gate_status"));
        assertEquals("AUTHORIZED", a.getProperty("authorization_status"));
        assertEquals(expectedSource, a.getProperty("source_commit"));
        assertEquals("pwd", a.getProperty("command"));
        assertEquals("POSIX_PWD", a.getProperty("command_semantics"));
        assertEquals("DEFERRED", a.getProperty("execution_status"));
        assertEquals("G17", a.getProperty("execution_authority"));
        assertEquals("DEFERRED:G17", a.getProperty("execution_path"));
        assertTrue(SHA256.matcher(a.getProperty("gate4_contract_sha256", "")).matches());
        assertTrue(SHA256.matcher(a.getProperty("profile_sha256", "")).matches());
        assertTrue(SHA256.matcher(a.getProperty("gate15_policy_sha256", "")).matches());
        assertTrue(SHA256.matcher(a.getProperty("gate5_authorization_sha256", "")).matches());
        String g16Hash = sha256(gate16);

        String script = "set -eu; " +
                "printf 'G17_GUEST_PROC_EXE='; readlink /proc/self/exe; " +
                "printf 'G17_GUEST_PROC_CWD='; readlink /proc/self/cwd; " +
                "printf 'G17_GUEST_PROC_ROOT='; readlink /proc/self/root; " +
                "printf 'G17_GUEST_PID=%s\\n' \"$$\"; " +
                "printf 'G17_COMMAND_RESULT='; pwd; " +
                "printf 'G17_COMMAND_RETURNCODE=0\\n'";
        List<String> command = new ArrayList<>();
        command.add(engine.getAbsolutePath()); command.add("-0"); command.add("-r"); command.add(rootfs.getAbsolutePath());
        command.add("-b"); command.add("/dev"); command.add("-b"); command.add("/proc"); command.add("-b"); command.add("/sys");
        command.add("-w"); command.add("/root"); command.add("/usr/bin/env"); command.add("-i");
        command.add("HOME=/root"); command.add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        command.add("TERM=xterm-256color"); command.add("PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath());
        command.add("/bin/sh"); command.add("-c"); command.add(script);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = readAll(process);
        int rc = process.waitFor();
        assertEquals("guest runtime execution failed: " + output, 0, rc);

        String guestExe = field(output, "G17_GUEST_PROC_EXE");
        String guestCwd = field(output, "G17_GUEST_PROC_CWD");
        String guestRoot = field(output, "G17_GUEST_PROC_ROOT");
        String guestPid = field(output, "G17_GUEST_PID");
        String result = field(output, "G17_COMMAND_RESULT");
        assertTrue("guest PID missing", guestPid.matches("[0-9]+"));
        assertEquals("/root", result);
        assertEquals("/root", guestCwd);
        assertTrue("guest /proc root missing", !guestRoot.isEmpty());
        assertTrue("guest /proc exe is not a real guest path", guestExe.startsWith("/"));

        File out = new File(context.getFilesDir(), "g17/gate17-runtime-execution.v1");
        if (!out.getParentFile().exists()) assertTrue(out.getParentFile().mkdirs());
        try (OutputStream stream = new FileOutputStream(out)) {
            String text = "schema_version=gate17-runtime-execution.v1\n" +
                    "gate=gate17\n" + "gate_status=PASS\n" + "execution_status=EXECUTED\n" +
                    "result_status=PASS\n" + "command=pwd\n" + "command_semantics=POSIX_PWD\n" +
                    "command_result=/root\n" + "command_returncode=0\n" +
                    "guest_pid=" + guestPid + "\n" + "guest_proc_exe=" + guestExe + "\n" +
                    "guest_proc_cwd=" + guestCwd + "\n" + "guest_proc_root=" + guestRoot + "\n" +
                    "android_package=" + PACKAGE + "\n" + "android_uid=" + uid + "\n" +
                    "android_selinux_context=" + selinux + "\n" +
                    "execution_engine=" + engine.getCanonicalPath() + "\n" +
                    "engine_sha256=" + sha256(engine) + "\n" +
                    "rootfs_path=" + rootfs.getCanonicalPath() + "\n" +
                    "rootfs_os_release_sha256=" + sha256(new File(rootfs, "etc/os-release")) + "\n" +
                    "pipeline_run_id=" + a.getProperty("pipeline_run_id") + "\n" +
                    "source_commit=" + expectedSource + "\n" +
                    "gate4_contract_sha256=" + a.getProperty("gate4_contract_sha256") + "\n" +
                    "profile_sha256=" + a.getProperty("profile_sha256") + "\n" +
                    "gate16_authorization_sha256=" + g16Hash + "\n" +
                    "gate16_authorization_id=" + a.getProperty("authorization_id") + "\n" +
                    "timestamp=" + System.currentTimeMillis() + "\n";
            stream.write(text.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("G17_LIVE_STATUS=PASS");
        System.out.println("G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION");
        System.out.println("G17_LIVE_PACKAGE=" + PACKAGE);
        System.out.println("G17_LIVE_UID=" + uid);
        System.out.println("G17_LIVE_SELINUX=" + selinux);
        System.out.println("G17_LIVE_ARTIFACT=" + out.getAbsolutePath());
    }

    private static String arg(String key) {
        Bundle b = InstrumentationRegistry.getArguments();
        String value = b.getString(key);
        if (value == null || value.isEmpty()) throw new AssertionError("missing instrumentation argument: " + key);
        return value;
    }
    private static String requiredArg(String key) { return arg(key); }
    private static Properties load(File file) throws Exception { Properties p = new Properties(); try (FileInputStream in = new FileInputStream(file)) { p.load(in); } return p; }
    private static String readFirstLine(File file) throws Exception { try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) { String s = r.readLine(); return s == null ? "" : s.trim(); } }
    private static String readAll(Process p) throws Exception { try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) { StringBuilder b = new StringBuilder(); String s; while ((s = r.readLine()) != null) b.append(s).append('\n'); return b.toString(); } }
    private static String field(String output, String key) { for (String line : output.split("\\R")) if (line.startsWith(key + "=")) return line.substring(key.length() + 1).trim(); throw new AssertionError("missing guest evidence: " + key + " output=" + output); }
    private static String sha256(File file) throws Exception { MessageDigest d = MessageDigest.getInstance("SHA-256"); byte[] b = new byte[65536]; int n; try (FileInputStream in = new FileInputStream(file)) { while ((n = in.read(b)) > 0) d.update(b, 0, n); } StringBuilder s = new StringBuilder(64); for (byte x : d.digest()) s.append(String.format("%02x", x & 255)); return s.toString(); }
}
