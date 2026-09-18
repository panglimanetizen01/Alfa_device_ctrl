package com.alfa.device_ctrl;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Atomic, fail-closed evidence sink for real external transport executions. */
public final class ExternalExecutionEvidence {
    private ExternalExecutionEvidence() {}

    public static File persist(Context context, String lane, String requestId, long pid,
                                String stdout, String stderr, int exitCode, String status) throws Exception {
        if (context == null) throw new IllegalArgumentException("context");
        if (lane == null || lane.trim().isEmpty()) throw new IllegalArgumentException("lane");
        if (requestId == null || requestId.trim().isEmpty()) throw new IllegalArgumentException("requestId");
        if (pid <= 0) throw new IllegalArgumentException("real pid required");
        File dir = new File(context.getFilesDir(), "external-evidence");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("evidence-dir-create-failed");
        File tmp = new File(dir, "." + requestId + "." + UUID.randomUUID() + ".tmp");
        File out = new File(dir, requestId + ".properties");
        String body = "lane=" + lane + "\n"
                + "request_id=" + requestId + "\n"
                + "pid=" + pid + "\n"
                + "stdout=" + encode(stdout) + "\n"
                + "stderr=" + encode(stderr) + "\n"
                + "exit=" + exitCode + "\n"
                + "status=" + status + "\n";
        try (FileOutputStream stream = new FileOutputStream(tmp)) {
            stream.write(body.getBytes(StandardCharsets.UTF_8));
            stream.getFD().sync();
        }
        if (!tmp.renameTo(out)) {
            tmp.delete();
            throw new IllegalStateException("evidence-atomic-rename-failed");
        }
        return out;
    }

    public static File persistFailure(Context context, String lane, String requestId, String reason) throws Exception {
        return persist(context, lane, requestId, Process.myPid(), "", reason == null ? "" : reason, 126, "FAILED");
    }

    private static String encode(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}