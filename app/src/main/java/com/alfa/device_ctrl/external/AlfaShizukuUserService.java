package com.alfa.device_ctrl.external;

import android.os.Parcel;
import android.os.Process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

/**
 * Runs one non-PTY external process inside the Shizuku UserService process.
 * The UserService itself is the privileged process boundary; the child process
 * inherits the UserService's shell/root identity.
 */
public final class AlfaShizukuUserService extends IAlfaShizukuService.Stub {
    private static final int DESTROY_TRANSACTION = 16777115;
    private static final String PID_WRAPPER = "echo $$; exec \"$0\" \"$@\"";

    private final Object lock = new Object();
    private Process process;
    private OutputStream stdin;
    private IAlfaShizukuCallback callback;

    @Override
    public void execute(String[] command, String workDir, IAlfaShizukuCallback remoteCallback) {
        if (command == null || command.length == 0 || command[0] == null || command[0].isEmpty()) {
            notifyError(remoteCallback, "empty command");
            return;
        }

        synchronized (lock) {
            if (process != null && process.isAlive()) {
                notifyError(remoteCallback, "an external process is already active");
                return;
            }
            callback = remoteCallback;
            try {
                List<String> argv = new ArrayList<>();
                argv.add("/system/bin/sh");
                argv.add("-c");
                argv.add(PID_WRAPPER);
                for (String arg : command) {
                    argv.add(arg);
                }

                ProcessBuilder builder = new ProcessBuilder(argv);
                if (workDir != null && !workDir.isEmpty()) {
                    builder.directory(new java.io.File(workDir));
                }
                builder.redirectErrorStream(false);
                process = builder.start();
                stdin = process.getOutputStream();
            } catch (Throwable error) {
                process = null;
                stdin = null;
                notifyError(remoteCallback, describe(error));
                return;
            }
        }

        final Process child = process;
        try {
            remoteCallback.asBinder().linkToDeath(() -> terminateProcess(child), 0);
        } catch (Throwable ignored) {
            // The process lifecycle still terminates normally if the callback cannot be linked.
        }

        Thread stdoutThread = new Thread(() -> readStdout(child, remoteCallback), "alfa-shizuku-stdout");
        Thread stderrThread = new Thread(() -> readStream(child.getErrorStream(), false, remoteCallback), "alfa-shizuku-stderr");
        Thread waitThread = new Thread(() -> awaitExit(child, remoteCallback), "alfa-shizuku-wait");
        stdoutThread.start();
        stderrThread.start();
        waitThread.start();
    }

    @Override
    public void writeStdin(String input) {
        synchronized (lock) {
            if (stdin == null || process == null || !process.isAlive()) {
                return;
            }
            try {
                stdin.write((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            } catch (IOException ignored) {
                // The exit/error callback remains the authoritative lifecycle evidence.
            }
        }
    }

    @Override
    public void terminate() {
        Process child;
        synchronized (lock) {
            child = process;
        }
        terminateProcess(child);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws android.os.RemoteException {
        if (code == DESTROY_TRANSACTION) {
            terminate();
            System.exit(0);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    private void readStdout(Process child, IAlfaShizukuCallback remoteCallback) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8))) {
            String pidLine = reader.readLine();
            long pid = Long.parseLong(pidLine == null ? "-1" : pidLine.trim());
            remoteCallback.onStarted(pid, Process.myUid());
            readStream(reader, true, remoteCallback);
        } catch (Throwable error) {
            notifyError(remoteCallback, describe(error));
        }
    }

    private void readStream(InputStream stream, boolean stdout, IAlfaShizukuCallback remoteCallback) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            readStream(reader, stdout, remoteCallback);
        } catch (Throwable error) {
            notifyError(remoteCallback, describe(error));
        }
    }

    private void readStream(BufferedReader reader, boolean stdout, IAlfaShizukuCallback remoteCallback) throws IOException {
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            String chunk = new String(buffer, 0, count);
            if (stdout) {
                remoteCallback.onStdout(chunk);
            } else {
                remoteCallback.onStderr(chunk);
            }
        }
    }

    private void awaitExit(Process child, IAlfaShizukuCallback remoteCallback) {
        try {
            int exitCode = child.waitFor();
            remoteCallback.onExit(exitCode);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            notifyError(remoteCallback, "wait interrupted");
        } finally {
            synchronized (lock) {
                if (process == child) {
                    process = null;
                    stdin = null;
                }
            }
        }
    }

    private void terminateProcess(Process child) {
        if (child == null) return;
        try {
            if (child.isAlive()) child.destroyForcibly();
        } catch (Throwable ignored) {
        }
    }

    private void notifyError(IAlfaShizukuCallback target, String message) {
        if (target == null) return;
        try {
            target.onError(message);
        } catch (Throwable ignored) {
        }
    }

    private String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
