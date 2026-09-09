package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;

public final class RuntimeProcessCleanupTest {
    @Test
    public void timeoutKillsProcessGroupChildren() throws Exception {
        File marker = File.createTempFile("alfa-runtime-child-", ".marker");
        assertTrue(marker.delete());
        String path = marker.getAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder(
                "sh", "-c", "sleep 3; printf child-survived > '" + path + "'");

        RuntimeSessionManager.CommandResult result =
                RuntimeSessionManager.executeProcess(builder, 100, 4096);

        assertTrue(result.timedOut);
        assertTrue(result.exitStatus == 124);
        Thread.sleep(500);
        assertFalse("child process survived timeout cleanup", marker.exists());
    }

    @Test
    public void normalProcessStillReturnsOutput() throws Exception {
        RuntimeSessionManager.CommandResult result = RuntimeSessionManager.executeProcess(
                new ProcessBuilder("sh", "-c", "printf hello"), 2000, 4096);
        assertFalse(result.timedOut);
        assertTrue(result.exitStatus == 0);
        assertTrue("hello".equals(result.output));
    }
}
