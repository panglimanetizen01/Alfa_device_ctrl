package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StitchV1RuntimeStateMachineTest {
    @Test public void ready_never_exposes_install() {
        StitchV1RuntimeStateMachine.State state = StitchV1RuntimeStateMachine.State.READY;
        assertFalse(StitchV1RuntimeStateMachine.showInstall(state));
        assertTrue(StitchV1RuntimeStateMachine.showOpen(state));
    }

    @Test public void non_ready_exposes_install() {
        for (StitchV1RuntimeStateMachine.State state : new StitchV1RuntimeStateMachine.State[]{
                StitchV1RuntimeStateMachine.State.NOT_INSTALLED,
                StitchV1RuntimeStateMachine.State.VERIFYING,
                StitchV1RuntimeStateMachine.State.FAILED}) {
            assertTrue(StitchV1RuntimeStateMachine.showInstall(state));
            assertFalse(StitchV1RuntimeStateMachine.showOpen(state));
        }
    }
}
