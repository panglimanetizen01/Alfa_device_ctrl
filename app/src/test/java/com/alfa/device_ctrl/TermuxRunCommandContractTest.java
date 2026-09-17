package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alfa.device_ctrl.external.TermuxRunCommandBridge;

import org.junit.Test;

public final class TermuxRunCommandContractTest {
    @Test
    public void runCommandTargetsTermuxService() {
        assertEquals("com.termux", TermuxRunCommandBridge.TERMUX_PACKAGE);
        assertEquals("com.termux.app.RunCommandService", TermuxRunCommandBridge.RUN_COMMAND_SERVICE);
        assertEquals("com.termux.RUN_COMMAND", TermuxRunCommandBridge.RUN_COMMAND_ACTION);
        assertEquals("com.termux.RUN_COMMAND_PATH", TermuxRunCommandBridge.EXTRA_COMMAND_PATH);
        assertEquals("com.termux.RUN_COMMAND_ARGUMENTS", TermuxRunCommandBridge.EXTRA_ARGUMENTS);
        assertEquals("com.termux.RUN_COMMAND_WORKDIR", TermuxRunCommandBridge.EXTRA_WORKDIR);
        assertEquals("com.termux.RUN_COMMAND_STDIN", TermuxRunCommandBridge.EXTRA_STDIN);
        assertEquals("com.termux.RUN_COMMAND_PENDING_INTENT", TermuxRunCommandBridge.EXTRA_PENDING_INTENT);
    }

    @Test
    public void resultSemanticsMatchDocumentedBackgroundExecution() {
        assertEquals("result", TermuxRunCommandBridge.RESULT_BUNDLE);
        assertEquals("stdout", TermuxRunCommandBridge.RESULT_STDOUT);
        assertEquals("stderr", TermuxRunCommandBridge.RESULT_STDERR);
        assertEquals("exitCode", TermuxRunCommandBridge.RESULT_EXIT_CODE);
        assertEquals("err", TermuxRunCommandBridge.RESULT_ERR);
        assertEquals("errmsg", TermuxRunCommandBridge.RESULT_ERRMSG);
        assertEquals(-1, TermuxRunCommandBridge.TERMUX_RESULT_OK);
        assertTrue(TermuxRunCommandBridge.TERMUX_SHELL.endsWith("/bin/sh"));
    }

    @Test
    public void termuxApiIsNotTheRunCommandLane() {
        assertTrue(ExecutionLane.TERMUX.isExternalToApp());
        assertTrue(ExecutionLane.TERMUX_API.isExternalToApp());
        assertEquals("com.termux.api", TermuxRunCommandBridge.TERMUX_API_PACKAGE);
        assertEquals("com.termux.permission.RUN_COMMAND", TermuxRunCommandBridge.RUN_COMMAND_PERMISSION);
    }
}
