package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alfa.device_ctrl.external.TermuxApiCapability;

import org.junit.Test;

public final class TermuxApiCapabilityTest {
    @Test
    public void capabilityIsSeparateFromTermuxExecutionLane() {
        assertEquals("com.termux.api", TermuxApiCapability.PACKAGE_NAME);
        assertTrue(ExecutionLane.TERMUX_API.isExternalToApp());
        assertTrue(ExecutionLane.TERMUX.isExternalToApp());
    }
}
