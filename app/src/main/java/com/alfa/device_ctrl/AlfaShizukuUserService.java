package com.alfa.device_ctrl;

import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.UserService;

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
            callback.onStarted(process.pid());
            Process p = process;
            Thread out = stream(p.getInputStream(), true, callback);
            Thread err = stream(p.getErrorStream(), false, callback);
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