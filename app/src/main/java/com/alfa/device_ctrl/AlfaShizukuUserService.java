package com.alfa.device_ctrl;

import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Real Shizuku UserService execution boundary. No simulated process/result values. */
public final class AlfaShizukuUserService extends IAlfaShizukuService.Stub {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override public void execute(String[] command, IAlfaShizukuCallback callback) throws RemoteException {
        if (command == null || command.length == 0 || callback == null) {
            if (callback != null) callback.onFailure("invalid-command");
            return;
        }
        executor.execute(() -> run(command, callback));
    }

    private void run(String[] command, IAlfaShizukuCallback callback) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(false).start();
            Process p = process;
            Thread out = stream(p.getInputStream(), true, callback);
            Thread err = stream(p.getErrorStream(), false, callback);
            long pid = findChildPid(process);
            if (pid <= 0) {
                callback.onFailure("FAILED_NO_PID");
                return;
            }
            callback.onStarted(pid);
            int exit = p.waitFor();
            out.join(5000);
            err.join(5000);
            callback.onExit(exit);
        } catch (Throwable error) {
            try { callback.onFailure(error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage())); }
            catch (RemoteException ignored) {}
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private static long findChildPid(Process process) {
        try {
            long parent = android.os.Process.myPid();
            java.io.File[] entries = new java.io.File("/proc").listFiles();
            if (entries == null) return 0;
            for (java.io.File entry : entries) {
                if (!entry.isDirectory()) continue;
                long candidate;
                try { candidate = Long.parseLong(entry.getName()); } catch (NumberFormatException ignored) { continue; }
                if (candidate <= 0 || candidate == parent) continue;
                java.nio.file.Path statPath = java.nio.file.Paths.get(entry.getAbsolutePath(), "stat");
                String stat = new String(java.nio.file.Files.readAllBytes(statPath), java.nio.charset.StandardCharsets.UTF_8);
                int close = stat.lastIndexOf(") ");
                if (close < 0) continue;
                String[] fields = stat.substring(close + 2).trim().split(" +");
                if (fields.length > 1 && Long.parseLong(fields[1]) == parent) return candidate;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static Thread stream(java.io.InputStream input, boolean stdout, IAlfaShizukuCallback callback) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    String chunk = new String(buffer, 0, n);
                    if (stdout) callback.onStdout(chunk); else callback.onStderr(chunk);
                }
            } catch (Throwable error) {
                try { callback.onFailure((stdout ? "stdout:" : "stderr:") + error.getClass().getSimpleName()); }
                catch (RemoteException ignored) {}
            }
        }, stdout ? "alfa-shizuku-stdout" : "alfa-shizuku-stderr");
        t.start();
        return t;
    }

    public void destroy() {
        executor.shutdownNow();
        System.exit(0);
    }
}