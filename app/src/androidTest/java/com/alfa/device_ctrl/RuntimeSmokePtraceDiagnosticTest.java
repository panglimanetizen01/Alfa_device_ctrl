package com.alfa.device_ctrl;

// Diagnostic-only harness: records live PRoot/tracee procfs state without changing the canonical smoke contract.
import android.content.Context;
import android.os.Process;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public final class RuntimeSmokePtraceDiagnosticTest {
    private static final String TAG = "G2-PROOT-DIAG";

    @Test
    public void capturePtraceStateUntilSmokeBoundary() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File vault = new File(c.getFilesDir(), "runtime-vault-ptrace-diagnostic");
        delete(vault);
        File lib = new File(c.getApplicationInfo().nativeLibraryDir).getCanonicalFile();
        File engine = new File(lib, "libproot.so").getCanonicalFile();
        RuntimeProfile profile = RuntimeRegistry.get("debian");
        File evidence = new File(c.getFilesDir(), "g2-proot-ptrace-diagnostic.txt");
        delete(evidence);

        RuntimeInstaller installer = new RuntimeInstaller(
                vault,
                m -> Log.i(TAG, m),
                engine,
                lib,
                (e, root) -> runDiagnostic(e, root, evidence));

        RuntimeInstaller.Result result = installer.install(
                profile,
                null,
                RuntimeInstaller.TRUSTED_PROOT_ARM64_SHA256,
                new URL(profile.rootfsUrl()),
                profile.rootfsSha256(),
                profile.rootfsGzip());

        Log.i(TAG, "INSTALL_RESULT success=" + result.success + " message=" + result.message);
        if (!result.success) throw new AssertionError(result.message);
    }

    private static String runDiagnostic(File engine, File root, File evidence) {
        Process p = null;
        long start = System.nanoTime();
        try {
            File tmp = new File(root.getParentFile(), "proot_tmp");
            File loader = new File(engine.getParentFile(), "libproot-loader.so");
            List<String> cmd = new ArrayList<>(Arrays.asList(
                    engine.getAbsolutePath(), "-0", "-r", root.getAbsolutePath(),
                    "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root",
                    "/usr/bin/env", "-i", "HOME=/root",
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                    "TERM=xterm-256color", "PROOT_TMP_DIR=" + tmp.getAbsolutePath(),
                    "PROOT_NO_SECCOMP=1", "/bin/sh", "-c",
                    "printf 'ALFA_RUNTIME_SMOKE_OK\\n'; id -u; pwd"));
            ProcessBuilder b = new ProcessBuilder(cmd);
            b.environment().put("PROOT_TMP_DIR", tmp.getAbsolutePath());
            b.environment().put("PROOT_LOADER", loader.getAbsolutePath());
            b.environment().put("PROOT_NO_SECCOMP", "1");
            b.environment().put("PROOT_VERBOSE", "9");
            b.directory(root.getParentFile());
            b.redirectErrorStream(true);
            File out = new File(evidence.getParentFile(), "g2-proot-verbose.log");
            b.redirectOutput(out);
            p = b.start();
            int ppid = Process.myPid();
            write(evidence, "parent_test_pid=" + ppid + "\nengine=" + engine + "\nroot=" + root + "\nstart_ns=" + start + "\n");

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(35);
            while (p.isAlive() && System.nanoTime() < deadline) {
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                append(evidence, "\n=== sample_ms=" + ms + " process_alive=" + p.isAlive() + " ===\n");
                List<Integer> roots = children(ppid);
                append(evidence, "test_children=" + roots + "\n");
                for (int pid : roots) {
                    dumpTree(pid, evidence, 0, new HashSet<>());
                }
                Thread.sleep(250);
            }
            boolean alive = p.isAlive();
            append(evidence, "\n=== boundary alive=" + alive + " elapsed_ms=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ===\n");
            if (alive) {
                p.destroyForcibly();
                p.waitFor(5, TimeUnit.SECONDS);
                return "diagnostic-timeout-35s";
            }
            String outText = read(out);
            append(evidence, "exit=" + p.exitValue() + "\noutput_tail=" + tail(outText, 8000) + "\n");
            if (p.exitValue() != 0) return "diagnostic-exit-" + p.exitValue();
            if (!outText.contains("ALFA_RUNTIME_SMOKE_OK")) return "diagnostic-contract-missing";
            return null;
        } catch (Exception e) {
            if (p != null) p.destroyForcibly();
            append(evidence, "exception=" + e + "\n");
            return "diagnostic-exception-" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    private static void dumpTree(int pid, File evidence, int depth, Set<Integer> seen) throws IOException {
        if (depth > 3 || !seen.add(pid)) return;
        append(evidence, "PID=" + pid + " depth=" + depth + "\n");
        append(evidence, readProc(pid, "status"));
        append(evidence, "wchan=" + readProcValue(pid, "wchan") + "\n");
        append(evidence, "syscall=" + readProcValue(pid, "syscall") + "\n");
        append(evidence, "cmdline=" + readProcValue(pid, "cmdline").replace('\0', ' ') + "\n");
        for (int child : children(pid)) dumpTree(child, evidence, depth + 1, seen);
    }

    private static String readProc(int pid, String name) throws IOException {
        return "--- /proc/" + pid + "/" + name + " ---\n" + readProcValue(pid, name) + "\n";
    }

    private static String readProcValue(int pid, String name) {
        try { return read(new File("/proc/" + pid + "/" + name)); }
        catch (Exception e) { return "<unreadable:" + e.getClass().getSimpleName() + ":" + e.getMessage() + ">"; }
    }

    private static List<Integer> children(int pid) {
        List<Integer> out = new ArrayList<>();
        String s = readProcValue(pid, "task/" + pid + "/children").trim();
        if (s.startsWith("<unreadable:")) return out;
        for (String x : s.split("\\s+")) if (!x.isEmpty()) try { out.add(Integer.parseInt(x)); } catch (NumberFormatException ignored) {}
        return out;
    }

    private static String read(File f) throws IOException { try (InputStream in = new FileInputStream(f)) { ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) b.write(buf,0,n); return b.toString(StandardCharsets.UTF_8.name()); } }
    private static String tail(String s, int n) { return s.length() <= n ? s : s.substring(s.length() - n); }
    private static void write(File f, String s) throws IOException { File p = f.getParentFile(); if (p != null) p.mkdirs(); try (FileOutputStream o = new FileOutputStream(f)) { o.write(s.getBytes(StandardCharsets.UTF_8)); } }
    private static void append(File f, String s) throws IOException { try (FileOutputStream o = new FileOutputStream(f, true)) { o.write(s.getBytes(StandardCharsets.UTF_8)); } }
    private static void delete(File f) { if (f == null || !f.exists()) return; if (f.isDirectory()) { File[] a=f.listFiles(); if(a!=null) for(File x:a) delete(x); } f.delete(); }
}
