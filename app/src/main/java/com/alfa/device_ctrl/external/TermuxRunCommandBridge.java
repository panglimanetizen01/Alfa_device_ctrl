package com.alfa.device_ctrl.external;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.concurrent.atomic.AtomicInteger;

/** Evidence-producing bridge for Termux's documented RUN_COMMAND external execution lane. */
public final class TermuxRunCommandBridge {
    public static final String TERMUX_PACKAGE = "com.termux";
    public static final String TERMUX_API_PACKAGE = "com.termux.api";
    public static final String RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND";
    public static final String RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService";
    public static final String RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND";
    public static final String EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH";
    public static final String EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS";
    public static final String EXTRA_STDIN = "com.termux.RUN_COMMAND_STDIN";
    public static final String EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR";
    public static final String EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND";
    public static final String EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT";
    public static final String RESULT_BUNDLE = "result";
    public static final String RESULT_STDOUT = "stdout";
    public static final String RESULT_STDERR = "stderr";
    public static final String RESULT_EXIT_CODE = "exitCode";
    public static final String RESULT_ERR = "err";
    public static final String RESULT_ERRMSG = "errmsg";
    public static final String TERMUX_PREFIX = "/data/data/com.termux/files/usr";
    public static final String TERMUX_SHELL = TERMUX_PREFIX + "/bin/sh";

    private static final AtomicInteger REQUEST_IDS = new AtomicInteger(1);
    private final Context context;

    public interface Callback {
        void onStarted(long pid);
        void onStdout(String output);
        void onStderr(String output);
        void onExit(int exitCode);
        void onError(String message);
    }

    public TermuxRunCommandBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isInstalled() {
        try {
            context.getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public boolean hasPermission() {
        return context.checkSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public void execute(String commandPath, String[] arguments, String workDir, String stdin, Callback callback) {
        if (!isInstalled()) {
            callback.onError("Termux package not installed");
            return;
        }
        if (!hasPermission()) {
            callback.onError("Termux RUN_COMMAND permission not granted");
            return;
        }
        if (commandPath == null || commandPath.isEmpty()) {
            callback.onError("empty command path");
            return;
        }

        final int requestId = REQUEST_IDS.getAndIncrement();
        TermuxResultReceiverService.register(requestId, callback);

        Intent callbackIntent = new Intent(context, TermuxResultReceiverService.class)
                .putExtra(TermuxResultReceiverService.EXTRA_REQUEST_ID, requestId);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getService(
                context, requestId, callbackIntent, pendingFlags);

        Intent intent = new Intent(RUN_COMMAND_ACTION);
        intent.setComponent(new ComponentName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE));
        intent.putExtra(EXTRA_COMMAND_PATH, TERMUX_SHELL);
        intent.putExtra(EXTRA_ARGUMENTS, wrapCommand(commandPath, arguments));
        if (stdin != null) intent.putExtra(EXTRA_STDIN, stdin);
        if (workDir != null && !workDir.isEmpty()) intent.putExtra(EXTRA_WORKDIR, workDir);
        intent.putExtra(EXTRA_BACKGROUND, false);
        intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent);

        try {
            context.startService(intent);
        } catch (Throwable error) {
            TermuxResultReceiverService.fail(requestId, describe(error));
        }
    }

    private String[] wrapCommand(String commandPath, String[] arguments) {
        int argCount = arguments == null ? 0 : arguments.length;
        String[] wrapped = new String[argCount + 3];
        wrapped[0] = "-c";
        wrapped[1] = "echo $$; exec \"$0\" \"$@\"";
        wrapped[2] = commandPath;
        if (arguments != null) {
            System.arraycopy(arguments, 0, wrapped, 3, arguments.length);
        }
        return wrapped;
    }

    private String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
