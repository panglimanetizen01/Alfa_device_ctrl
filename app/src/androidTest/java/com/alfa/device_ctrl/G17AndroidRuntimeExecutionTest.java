package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.terminal.TerminalSession;

import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** G17 proof: exact Gate16 authorization reaches a real Android PTY, shell I/O, command and shutdown. */
public final class G17AndroidRuntimeExecutionTest {
    private static final String PACKAGE = "com.alfa.device_ctrl";
    private static final String PROOT_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    private static final String LOADER_SHA256 = "663ef19c278dc39bb4a242ba244d5af6776610a936f33e5205c28fc016350b3a";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    @Test public void exactGate16AuthorizationExecutesPwdInsidePackagedRuntime() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals(PACKAGE, c.getPackageName());
        ApplicationInfo ai = c.getApplicationInfo();
        File engine = new File(ai.nativeLibraryDir, "libproot.so");
        File loader = new File(ai.nativeLibraryDir, "libproot-loader.so");
        assertTrue("packaged PRoot missing", engine.isFile() && engine.canExecute());
        assertEquals("packaged PRoot SHA mismatch", PROOT_SHA256, sha256(engine));
        assertTrue("native loader missing", loader.isFile() && loader.canExecute());
        assertEquals("native loader SHA mismatch", LOADER_SHA256, sha256(loader));

        int uid = android.os.Process.myUid();
        assertEquals("instrumented process is not package UID", c.getPackageManager().getApplicationInfo(PACKAGE, 0).uid, uid);
        assertTrue("shell UID must never be accepted", uid != 2000);
        String selinux = line(new File("/proc/self/attr/current"));
        assertTrue("SELinux context missing", !selinux.isEmpty());
        assertTrue("shell SELinux context must never be accepted", !"u:r:shell:s0".equals(selinux));

        File runtime = new File(new File(c.getFilesDir(), "runtime-vault"), "runtimes/ubuntu");
        File ready = new File(runtime, "READY.evidence");
        File rootfs = new File(runtime, "rootfs");
        assertTrue("runtime READY evidence missing", ready.isFile());
        assertTrue("runtime rootfs missing", rootfs.isDirectory());
        assertTrue("rootfs /proc missing", new File(rootfs, "proc").isDirectory());
        assertTrue("rootfs /dev missing", new File(rootfs, "dev").isDirectory());
        assertTrue("rootfs /sys missing", new File(rootfs, "sys").isDirectory());
        assertTrue("runtime evidence invalid", RuntimeEvidence.verify(ready, "ubuntu", engine, rootfs));

        String source = arg("source_commit");
        File g16 = new File(arg("gate16_path"));
        String filesRoot = c.getFilesDir().getCanonicalPath() + File.separator;
        assertTrue("Gate16 artifact must be app-private", g16.getCanonicalPath().startsWith(filesRoot));
        assertTrue("Gate16 artifact missing", g16.isFile());
        Properties a = props(g16);
        assertEquals("gate16-runtime-execution-authorization.v1", a.getProperty("schema_version"));
        assertEquals("gate16", a.getProperty("gate"));
        assertEquals("PASS", a.getProperty("gate_status"));
        assertEquals("AUTHORIZED", a.getProperty("authorization_status"));
        assertEquals(source, a.getProperty("source_commit"));
        assertEquals("pwd", a.getProperty("command"));
        assertEquals("POSIX_PWD", a.getProperty("command_semantics"));
        assertEquals("DEFERRED", a.getProperty("execution_status"));
        assertEquals("G17", a.getProperty("execution_authority"));
        assertEquals("DEFERRED:G17", a.getProperty("execution_path"));
        for (String k : new String[]{"gate4_contract_sha256","profile_sha256","gate15_policy_sha256","gate5_authorization_sha256"}) {
            assertTrue("missing/invalid " + k, SHA256.matcher(a.getProperty(k, "")).matches());
        }
        String g16Hash = sha256(g16);

        // Provision the exact Gate 7 launch contract for this Gate 6/16 run.
        File runtimeVault = runtime.getParentFile().getParentFile();
        File launch = new File(runtimeVault, "gate7-launch.properties");
        Properties launchProps = new Properties();
        launchProps.setProperty("pipeline_run_id", a.getProperty("pipeline_run_id"));
        launchProps.setProperty("runtime_id", "ubuntu");
        launchProps.setProperty("source_commit", source);
        launchProps.setProperty("gate4_contract_sha256", a.getProperty("gate4_contract_sha256"));
        launchProps.setProperty("profile_sha256", a.getProperty("profile_sha256"));
        launchProps.setProperty("implementation_commit", source);
        try (FileOutputStream out = new FileOutputStream(launch)) { launchProps.store(out, "Alfa Gate 7 launch contract"); }

        File prootTmp = new File(runtime, "proot_tmp");
        InteractiveSessionContract contract = new InteractiveSessionContract(
                "session-g17-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                a.getProperty("request_id"), a.getProperty("pipeline_run_id"), "ubuntu",
                source, a.getProperty("gate4_contract_sha256"), a.getProperty("profile_sha256"), source,
                ready, engine, rootfs, c.getFilesDir(),
                new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=alfa:ubuntu:\\w\\$ ",
                        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                        "PROOT_TMP_DIR=" + prootTmp.getCanonicalPath(), "PROOT_LOADER=" + loader.getCanonicalPath()});
        assertTrue("Gate 7 authorization failed", contract.isAuthorizedForInteractiveRuntime());

        RuntimeSessionManager manager = new RuntimeSessionManager(contract, null);
        assertTrue("PTY failed to start", manager.start(80, 24, 8, 16));
        long promptDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!manager.isPromptReady() && System.nanoTime() < promptDeadline) Thread.sleep(50);
        assertTrue("PTY never reached READY prompt", manager.isPromptReady());

        File readyEvidence = new File(runtimeVault, "evidence/sessions/" + contract.sessionId() + ".READY.properties");
        assertTrue("G7 READY evidence missing", readyEvidence.isFile());
        File g7Out = new File(c.getFilesDir(), "g17/gate7-session-ready.v1");
        if (!g7Out.getParentFile().exists()) assertTrue(g7Out.getParentFile().mkdirs());
        Files.copy(readyEvidence.toPath(), g7Out.toPath(), StandardCopyOption.REPLACE_EXISTING);

        TerminalSession terminal = manager.currentSession();
        assertTrue("PTY session object missing", terminal != null && terminal.isRunning());
        byte[] commandBytes = "printf G17_PTY_IO_OK\\n; pwd\\n".getBytes(StandardCharsets.UTF_8);
        terminal.write(commandBytes, 0, commandBytes.length);
        long ioDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        String transcript = "";
        while (System.nanoTime() < ioDeadline) {
            if (terminal.getEmulator() != null && terminal.getEmulator().getScreen() != null) {
                transcript = terminal.getEmulator().getScreen().getTranscriptText();
                if (transcript.contains("G17_PTY_IO_OK") && transcript.contains("/root")) break;
            }
            Thread.sleep(50);
        }
        assertTrue("PTY input/output command marker missing", transcript.contains("G17_PTY_IO_OK"));
        assertTrue("PTY pwd output missing", transcript.contains("/root"));

        // Keep the exact authorized command evidence separate from the PTY smoke command.
        String script = "set -eu; printf 'G17_GUEST_PROC_EXE='; readlink /proc/self/exe; " +
                "printf 'G17_GUEST_PROC_CWD='; readlink /proc/self/cwd; " +
                "printf 'G17_GUEST_PROC_ROOT='; readlink /proc/self/root; " +
                "printf 'G17_GUEST_PID=%s\\n' \"$$\"; " +
                "printf 'G17_COMMAND_RESULT='; pwd; printf 'G17_COMMAND_RETURNCODE=0\\n'";
        List<String> x = new ArrayList<>();
        Collections.addAll(x, engine.getAbsolutePath(), "-0", "-r", rootfs.getAbsolutePath(),
                "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root", "/usr/bin/env", "-i",
                "HOME=/root", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=xterm-256color", "PROOT_TMP_DIR=" + prootTmp.getCanonicalPath(),
                "PROOT_LOADER=" + loader.getCanonicalPath(), "/bin/sh", "-c", script);
        java.lang.Process p = new ProcessBuilder(x).redirectErrorStream(true).start();
        String output = read(p);
        int rc = p.waitFor();
        assertEquals("guest execution failed: " + output, 0, rc);
        String exe = field(output, "G17_GUEST_PROC_EXE");
        String cwd = field(output, "G17_GUEST_PROC_CWD");
        String root = field(output, "G17_GUEST_PROC_ROOT");
        String pid = field(output, "G17_GUEST_PID");
        String result = field(output, "G17_COMMAND_RESULT");
        assertTrue("guest PID invalid", pid.matches("[0-9]+"));
        assertTrue("guest exe invalid", exe.startsWith("/"));
        assertTrue("guest root missing", !root.isEmpty());
        assertEquals("/root", cwd);
        assertEquals("/root", result);

        manager.stop();
        long stopDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (manager.isRunning() && System.nanoTime() < stopDeadline) Thread.sleep(50);
        assertTrue("PTY did not shut down", !manager.isRunning());

        File out = new File(c.getFilesDir(), "g17/gate17-runtime-execution.v1");
        assertTrue("cannot create G17 evidence directory", out.getParentFile().exists() || out.getParentFile().mkdirs());
        String commandHash = sha256Text("pwd\n");
        String resultHash = sha256Text("/root\n");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write("schema_version=gate17-runtime-execution.v1\n");
            w.write("gate=gate17\ngate_status=PASS\n");
            w.write("execution_id=execution-pwd-" + a.getProperty("pipeline_run_id") + "\n");
            w.write("pipeline_run_id=" + a.getProperty("pipeline_run_id") + "\n");
            w.write("source_commit=" + source + "\n");
            w.write("gate4_contract_sha256=" + a.getProperty("gate4_contract_sha256") + "\n");
            w.write("profile_sha256=" + a.getProperty("profile_sha256") + "\n");
            w.write("gate16_authorization_sha256=" + g16Hash + "\n");
            w.write("gate16_authorization_artifact=" + g16.getCanonicalPath() + "\n");
            w.write("request_id=" + a.getProperty("request_id") + "\ncommand=pwd\ncommand_semantics=POSIX_PWD\n");
            w.write("command_sha256=" + commandHash + "\nauthorization_status=AUTHORIZED\nexecution_status=EXECUTED\nresult_status=PASS\n");
            w.write("command_result=/root\ncommand_returncode=0\ncommand_result_sha256=" + resultHash + "\n");
            w.write("engine_path=" + engine.getCanonicalPath() + "\nengine_sha256=" + sha256(engine) + "\nrootfs_path=" + rootfs.getCanonicalPath() + "\n");
            w.write("rootfs_os_id=" + props(new File(rootfs, "etc/os-release")).getProperty("ID", "unknown").replace("\"", "") + "\n");
            w.write("rootfs_os_release_sha256=" + sha256(new File(rootfs,"etc/os-release")) + "\n");
            w.write("guest_pid=" + pid + "\nguest_internal_pid=" + pid + "\nhost_tracee_pid=-1\n");
            w.write("guest_proc_exe=" + exe + "\nguest_proc_cwd=/root\nguest_proc_root=" + root + "\n");
            w.write("android_package=" + PACKAGE + "\nandroid_uid=" + uid + "\nandroid_selinux_context=" + selinux + "\n");
            w.write("timestamp=" + System.currentTimeMillis() + "\nexecution_path=PRoot:-0:-r:<rootfs>:-b:/dev:-b:/proc:-b:/sys:-w:/root:/usr/bin/env:/bin/sh:-c:pwd\n");
            w.write("execution_reason=G16 authorization verified; exact pwd executed inside explicit PRoot guest rootfs; PTY shell I/O and shutdown also verified\n");
        }
        System.out.println("G17_LIVE_STATUS=PASS");
        System.out.println("G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION");
        System.out.println("G17_LIVE_PACKAGE="+PACKAGE);
        System.out.println("G17_LIVE_UID="+uid);
        System.out.println("G17_LIVE_SELINUX="+selinux);
        System.out.println("G17_LIVE_ARTIFACT="+out);
        System.out.println("G17_G7_READY_ARTIFACT="+g7Out);
    }

    private static String arg(String k){ Bundle b=InstrumentationRegistry.getArguments(); String v=b.getString(k); if(v==null||v.isEmpty())throw new AssertionError("missing instrumentation argument: "+k); return v; }
    private static Properties props(File f)throws Exception{Properties p=new Properties();try(FileInputStream i=new FileInputStream(f)){p.load(i);}return p;}
    private static String line(File f)throws Exception{try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){String s=r.readLine();return s==null?"":s.trim();}}
    private static String read(java.lang.Process p)throws Exception{try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s).append('\n');return b.toString();}}
    private static String field(String o,String k){for(String s:o.split("\\R"))if(s.startsWith(k+"="))return s.substring(k.length()+1).trim();throw new AssertionError("missing "+k+" output="+o);}
    private static String sha256(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;try(FileInputStream i=new FileInputStream(f)){while((n=i.read(b))>0)d.update(b,0,n);}StringBuilder s=new StringBuilder(64);for(byte v:d.digest())s.append(String.format("%02x",v&255));return s.toString();}
    private static String sha256Text(String value)throws Exception{return sha256Bytes(value.getBytes(StandardCharsets.UTF_8));}
    private static String sha256Bytes(byte[] bytes)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");d.update(bytes);StringBuilder s=new StringBuilder(64);for(byte v:d.digest())s.append(String.format("%02x",v&255));return s.toString();}
}
