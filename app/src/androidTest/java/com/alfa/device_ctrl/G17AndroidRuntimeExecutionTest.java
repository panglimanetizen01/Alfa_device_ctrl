package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/** G17 proof: exact Gate16 authorization is executed by the instrumented app process. */
public final class G17AndroidRuntimeExecutionTest {
    private static final String PACKAGE = "com.alfa.device_ctrl";
    private static final String PROOT_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    @Test public void exactGate16AuthorizationExecutesPwdInsidePackagedRuntime() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals(PACKAGE, c.getPackageName());
        ApplicationInfo ai = c.getApplicationInfo();
        File engine = new File(ai.nativeLibraryDir, "libproot.so");
        File loader = new File(ai.nativeLibraryDir, "libproot-loader.so");
        assertTrue("packaged PRoot missing", engine.isFile() && engine.canExecute());
        assertEquals("packaged PRoot SHA mismatch", PROOT_SHA256, sha256(engine));
        assertTrue("packaged PRoot loader missing", loader.isFile() && loader.canExecute());
        assertEquals("packaged PRoot loader SHA mismatch", RuntimeEvidence.TRUSTED_PROOT_LOADER_ARM64_SHA256, sha256(loader));

        int uid = android.os.Process.myUid();
        assertEquals("instrumented process is not package UID", c.getPackageManager().getApplicationInfo(PACKAGE, 0).uid, uid);
        assertTrue("shell UID must never be accepted", uid != 2000);
        String selinux = line(new File("/proc/self/attr/current"));
        assertTrue("SELinux context missing", !selinux.isEmpty());
        assertTrue("shell SELinux context must never be accepted", !"u:r:shell:s0".equals(selinux));

        String runtimeId = RuntimeSelection.DEFAULT_RUNTIME_ID;
        File runtime = new File(new File(c.getFilesDir(), "runtime-vault"), "runtimes/" + runtimeId);
        File ready = new File(runtime, "READY.evidence");
        File rootfs = new File(runtime, "rootfs");
        assertTrue("runtime READY evidence missing", ready.isFile());
        assertTrue("runtime rootfs missing", rootfs.isDirectory());
        assertTrue("rootfs /proc missing", new File(rootfs, "proc").isDirectory());
        assertTrue("rootfs /dev missing", new File(rootfs, "dev").isDirectory());
        assertTrue("rootfs /sys missing", new File(rootfs, "sys").isDirectory());
        assertTrue("runtime evidence invalid", RuntimeEvidence.verify(ready, runtimeId, engine, rootfs));

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

        String script = "set -eu; printf 'G17_GUEST_PROC_EXE='; readlink /proc/self/exe; " +
                "printf 'G17_GUEST_PROC_CWD='; readlink /proc/self/cwd; " +
                "printf 'G17_GUEST_PROC_ROOT='; readlink /proc/self/root; " +
                "printf 'G17_GUEST_PID=%s\\n' \"$$\"; " +
                "printf 'G17_COMMAND_RESULT='; pwd; printf 'G17_COMMAND_RETURNCODE=0\\n'";
        List<String> x = new ArrayList<>();
        Collections.addAll(x, engine.getAbsolutePath(), "-0", "-r", rootfs.getAbsolutePath(),
                "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root", "/usr/bin/env", "-i",
                "HOME=/root", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=xterm-256color", "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath(),
                "PROOT_LOADER=" + loader.getAbsolutePath(), "/bin/sh", "-c", script);
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

        File out = new File(c.getFilesDir(), "g17/gate17-runtime-execution.v1");
        assertTrue("cannot create G17 evidence directory", out.getParentFile().exists() || out.getParentFile().mkdirs());
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write("schema_version=gate17-runtime-execution.v1\n");
            w.write("gate=gate17\n gate_status=PASS\n".replace(" ", ""));
            w.write("execution_status=EXECUTED\nresult_status=PASS\ncommand=pwd\ncommand_semantics=POSIX_PWD\ncommand_result=/root\ncommand_returncode=0\n");
            w.write("guest_pid="+pid+"\nguest_proc_exe="+exe+"\nguest_proc_cwd="+cwd+"\nguest_proc_root="+root+"\n");
            w.write("android_package="+PACKAGE+"\nandroid_uid="+uid+"\nandroid_selinux_context="+selinux+"\n");
            w.write("execution_engine="+engine.getCanonicalPath()+"\nengine_sha256="+sha256(engine)+"\nrootfs_path="+rootfs.getCanonicalPath()+"\nrootfs_os_release_sha256="+sha256(new File(rootfs,"etc/os-release"))+"\n");
            w.write("pipeline_run_id="+a.getProperty("pipeline_run_id")+"\nsource_commit="+source+"\ngate4_contract_sha256="+a.getProperty("gate4_contract_sha256")+"\nprofile_sha256="+a.getProperty("profile_sha256")+"\ngate16_authorization_sha256="+g16Hash+"\ngate16_authorization_id="+a.getProperty("authorization_id")+"\ntimestamp="+System.currentTimeMillis()+"\n");
        }
        System.out.println("G17_LIVE_STATUS=PASS");
        System.out.println("G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION");
        System.out.println("G17_LIVE_PACKAGE="+PACKAGE);
        System.out.println("G17_LIVE_UID="+uid);
        System.out.println("G17_LIVE_SELINUX="+selinux);
        System.out.println("G17_LIVE_ARTIFACT="+out);
    }

    private static String arg(String k){ Bundle b=InstrumentationRegistry.getArguments(); String v=b.getString(k); if(v==null||v.isEmpty())throw new AssertionError("missing instrumentation argument: "+k); return v; }
    private static Properties props(File f)throws Exception{Properties p=new Properties();try(FileInputStream i=new FileInputStream(f)){p.load(i);}return p;}
    private static String line(File f)throws Exception{try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){String s=r.readLine();return s==null?"":s.trim();}}
    private static String read(java.lang.Process p)throws Exception{try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s).append('\n');return b.toString();}}
    private static String field(String o,String k){for(String s:o.split("\\R"))if(s.startsWith(k+"="))return s.substring(k.length()+1).trim();throw new AssertionError("missing "+k+" output="+o);}
    private static String sha256(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;try(FileInputStream i=new FileInputStream(f)){while((n=i.read(b))>0)d.update(b,0,n);}StringBuilder s=new StringBuilder(64);for(byte v:d.digest())s.append(String.format("%02x",v&255));return s.toString();}
}
