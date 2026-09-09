package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class RuntimeSessionManagerTest {
    @Test
    public void executeProcessTimesOutAndKillsChild() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("sh", "-c", "sleep 2; echo SHOULD_NOT_COMPLETE");

        RuntimeSessionManager.CommandResult result = RuntimeSessionManager.executeProcess(builder, 100, 12000);

        assertTrue(result.timedOut);
        assertFalse(result.output.contains("SHOULD_NOT_COMPLETE"));
    }

    @Test
    public void executeProcessCapturesOutputWithoutTimeout() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("sh", "-c", "printf 'ALFA_OK'");

        RuntimeSessionManager.CommandResult result = RuntimeSessionManager.executeProcess(builder, 5000, 12000);

        assertFalse(result.timedOut);
        assertTrue(result.output.contains("ALFA_OK"));
    }
}
