package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

public final class OperationEvidenceTest {
    @Test
    public void invalidPidCannotProducePtyPass() throws Exception {
        Method probe = OperationEvidence.class.getDeclaredMethod("probePty", int.class);
        probe.setAccessible(true);
        Object proof = probe.invoke(null, -1);
        Field status = proof.getClass().getDeclaredField("status");
        status.setAccessible(true);
        assertEquals("NOT_MEASURED", status.get(proof));
    }
}
