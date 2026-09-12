package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Fresh-install acceptance proof: MainActivity must reach a real PRoot prompt and execute pwd as /root. */
@RunWith(AndroidJUnit4.class)
public final class AlfaStartupTerminalE2ETest {
    private static final long STARTUP_TIMEOUT_MS = 180000L;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    @Test public void freshStartupReachesTerminalPromptAndExecutesPwd() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo info = context.getApplicationInfo();
        String expectedApkSha256 = arg("expected_apk_sha256").toLowerCase(Locale.ROOT);
        assertTrue("expected APK SHA invalid", SHA256.matcher(expectedApkSha256).matches());
        assertTrue("installed APK source missing", new File(info.sourceDir).isFile());
        assertEquals("installed APK does not match exact CI artifact", expectedApkSha256, sha256(new File(info.sourceDir)));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            long deadline = SystemClock.uptimeMillis() + STARTUP_TIMEOUT_MS;
            AtomicReference<RuntimeSessionManager> managerRef = new AtomicReference<>();
            AtomicReference<String> transcriptRef = new AtomicReference<>("");

            while (SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity(activity -> {
                    RuntimeSessionManager manager = readManager(activity);
                    managerRef.set(manager);
                    if (manager != null && manager.currentSession() != null && manager.currentSession().getEmulator() != null
                            && manager.currentSession().getEmulator().getScreen() != null) {
                        transcriptRef.set(manager.currentSession().getEmulator().getScreen().getTranscriptText());
                    }
                });
                RuntimeSessionManager manager = managerRef.get();
                if (manager != null && manager.isRunning() && manager.isPromptReady()) break;
                SystemClock.sleep(250L);
            }

            RuntimeSessionManager manager = managerRef.get();
            assertNotNull("runtime session manager was never created", manager);
            assertTrue("runtime session did not become running", manager.isRunning());
            assertTrue("runtime prompt was never observed: " + transcriptRef.get(), manager.isPromptReady());
            assertNotNull("terminal session was not created", manager.currentSession());
            assertTrue("terminal PTY pid invalid", manager.currentSession().getPid() > 0);
            assertTrue("Alfa prompt was not rendered: " + transcriptRef.get(), transcriptRef.get().contains("alfa:debian:"));

            AtomicReference<File> readyRef = new AtomicReference<>();
            scenario.onActivity(activity -> {
                File runtime = new File(new File(activity.getFilesDir(), "runtime-vault"), "runtimes/debian");
                readyRef.set(new File(runtime, "READY.evidence"));
            });
            assertTrue("runtime-ready.v1 evidence missing", readyRef.get() != null && readyRef.get().isFile());

            CountDownLatch commandDone = new CountDownLatch(1);
            AtomicReference<String> output = new AtomicReference<>("");
            AtomicInteger exitCode = new AtomicInteger(126);
            manager.runRuntimeCommand("pwd", (text, code) -> {
                output.set(text == null ? "" : text);
                exitCode.set(code);
                commandDone.countDown();
            });
            assertTrue("pwd runtime command timed out", commandDone.await(35, TimeUnit.SECONDS));
            assertEquals("pwd inside runtime must exit 0", 0, exitCode.get());
            assertEquals("/root", output.get().trim());

            System.out.println("ALFA_STARTUP_TERMINAL_E2E=PASS");
            System.out.println("ALFA_STARTUP_APK_SHA256=" + expectedApkSha256);
            System.out.println("ALFA_RUNTIME_PROMPT=alfa:debian:");
            System.out.println("ALFA_RUNTIME_PWD=/root");
            System.out.println("ALFA_PTY_PID=" + manager.currentSession().getPid());
        }
    }

    private static String arg(String key) {
        Bundle args = InstrumentationRegistry.getArguments();
        String value = args.getString(key);
        if (value == null || value.isEmpty()) throw new AssertionError("missing instrumentation argument: " + key);
        return value;
    }

    private static RuntimeSessionManager readManager(Activity activity) {
        try {
            Field field = MainActivity.class.getDeclaredField("sessionManager");
            field.setAccessible(true);
            return (RuntimeSessionManager) field.get(activity);
        } catch (Exception error) {
            return null;
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[65536];
        int count;
        try (FileInputStream input = new FileInputStream(file)) {
            while ((count = input.read(buffer)) > 0) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) result.append(String.format("%02x", value & 255));
        return result.toString();
    }
}
