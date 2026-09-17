package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alfa.device_ctrl.external.ExternalExecutionResult;
import com.alfa.device_ctrl.external.ExternalExecutionTransport;
import com.alfa.device_ctrl.external.ShizukuExecutionBridge;

import org.junit.Test;

public final class ExternalBridgeContractTest {
    @Test
    public void externalExecutionResultPreservesCausalEvidence() {
        ExternalExecutionResult result = new ExternalExecutionResult(
                ExecutionLane.SHIZUKU_RISH,
                4242L,
                0,
                "stdout\n",
                "",
                null);

        assertTrue(result.isProcessStarted());
        assertTrue(result.isOutputCaptured());
        assertTrue(result.isSuccessful());
        assertFalse(result.hasError());
        assertTrue(result.getLane() == ExecutionLane.SHIZUKU_RISH);
        assertTrue(result.getPid() == 4242L);
    }

    @Test
    public void externalTransportContractSeparatesRuntimeAndExternalLanes() {
        assertTrue(ExecutionLane.SHIZUKU_RISH.isExternalToApp());
        assertTrue(ExecutionLane.TERMUX.isExternalToApp());
        assertTrue(ExecutionLane.TERMUX_API.isExternalToApp());
        assertFalse(ExecutionLane.RUNTIME.isExternalToApp());
        assertTrue(ExternalExecutionTransport.class.isInterface());
    }

    @Test
    public void shizukuPermissionRequestHasStableApplicationCode() {
        assertTrue(ShizukuExecutionBridge.REQUEST_PERMISSION_CODE > 0);
    }
}
