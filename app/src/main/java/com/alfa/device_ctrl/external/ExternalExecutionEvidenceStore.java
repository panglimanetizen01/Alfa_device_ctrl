package com.alfa.device_ctrl.external;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Persists external process evidence on the DUT for post-run verification. */
public final class ExternalExecutionEvidenceStore {
    private ExternalExecutionEvidenceStore() { }

    public static File persist(Context context, ExternalExecutionResult result, String marker) throws Exception {
        File dir = new File(context.getFilesDir(), "external-evidence");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("external-evidence-dir-create-failed");
        String safeLane = result.getLane().name().toLowerCase(java.util.Locale.ROOT);
        File file = new File(dir, System.currentTimeMillis() + "-" + safeLane + ".properties");
        StringBuilder out = new StringBuilder();
        out.append("schema=external-execution-v1\n");
        out.append("marker=").append(escape(marker)).append('\n');
        out.append("lane=").append(result.getLane()).append('\n');
        out.append("pid=").append(result.getPid()).append('\n');
        out.append("exit_code=").append(result.getExitCode()).append('\n');
        out.append("error=").append(escape(result.getError())).append('\n');
        out.append("stdout=").append(escape(result.getStdout())).append('\n');
        out.append("stderr=").append(escape(result.getStderr())).append('\n');
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(out.toString().getBytes(StandardCharsets.UTF_8));
            stream.getFD().sync();
        }
        return file;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}
