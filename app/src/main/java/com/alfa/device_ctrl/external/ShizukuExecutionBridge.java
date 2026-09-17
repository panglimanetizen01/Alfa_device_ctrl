package com.alfa.device_ctrl.external;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import rikka.shizuku.Shizuku;

/** App-side lifecycle and evidence bridge for the Shizuku UserService lane. */
public final class ShizukuExecutionBridge {
    public static final int REQUEST_PERMISSION_CODE = 4201;

    public interface Callback {
        void onStarted(long pid, int uid);
        void onStdout(String chunk);
        void onStderr(String chunk);
        void onExit(int exitCode);
        void onError(String message);
    }

    private final Context context;
    private final Object lock = new Object();
    private Shizuku.UserServiceArgs userServiceArgs;
    private IAlfaShizukuService service;
    private Callback callback;

    public ShizukuExecutionBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isAvailable() {
        return Shizuku.pingBinder();
    }

    public boolean hasPermission() {
        if (!isAvailable()) return false;
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public void requestPermission() {
        if (!isAvailable()) return;
        Shizuku.requestPermission(REQUEST_PERMISSION_CODE);
    }

    public void execute(String[] command, String workDir, Callback callback) {
        if (!isAvailable()) {
            callback.onError("Shizuku binder unavailable");
            return;
        }
        if (!hasPermission()) {
            callback.onError("Shizuku permission not granted");
            return;
        }
        if (command == null || command.length == 0) {
            callback.onError("empty command");
            return;
        }

        synchronized (lock) {
            if (service != null) {
                callback.onError("Shizuku UserService already active");
                return;
            }
            this.callback = callback;
            userServiceArgs = new Shizuku.UserServiceArgs(
                    new ComponentName(context, AlfaShizukuUserService.class))
                    .daemon(false)
                    .version(1)
                    .tag("alfa-external-exec-v1")
                    .processNameSuffix("external-exec");
        }

        final Shizuku.UserServiceArgs args = userServiceArgs;
        final ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                IAlfaShizukuService remote = IAlfaShizukuService.Stub.asInterface(binder);
                synchronized (lock) {
                    service = remote;
                }
                try {
                    remote.execute(command, workDir, new IAlfaShizukuCallback.Stub() {
                        @Override
                        public void onStarted(long pid, int uid) {
                            callback.onStarted(pid, uid);
                        }

                        @Override
                        public void onStdout(String chunk) {
                            callback.onStdout(chunk);
                        }

                        @Override
                        public void onStderr(String chunk) {
                            callback.onStderr(chunk);
                        }

                        @Override
                        public void onExit(int exitCode) {
                            callback.onExit(exitCode);
                            cleanup(args, connection);
                        }

                        @Override
                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
                } catch (Throwable error) {
                    callback.onError(describe(error));
                    cleanup(args, connection);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                synchronized (lock) {
                    service = null;
                }
                callback.onError("Shizuku UserService disconnected");
            }
        };

        try {
            Shizuku.bindUserService(args, connection);
        } catch (Throwable error) {
            callback.onError(describe(error));
            cleanup(args, connection);
        }
    }

    public void writeStdin(String input) {
        IAlfaShizukuService remote;
        synchronized (lock) {
            remote = service;
        }
        if (remote == null) return;
        try {
            remote.writeStdin(input);
        } catch (Throwable error) {
            Callback target;
            synchronized (lock) {
                target = callback;
            }
            if (target != null) target.onError(describe(error));
        }
    }

    public void terminate() {
        IAlfaShizukuService remote;
        synchronized (lock) {
            remote = service;
        }
        if (remote == null) return;
        try {
            remote.terminate();
        } catch (Throwable ignored) {
        }
    }

    private void cleanup(Shizuku.UserServiceArgs args, ServiceConnection connection) {
        try {
            Shizuku.unbindUserService(args, connection, true);
        } catch (Throwable ignored) {
        }
        synchronized (lock) {
            service = null;
            callback = null;
            userServiceArgs = null;
        }
    }

    private String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
