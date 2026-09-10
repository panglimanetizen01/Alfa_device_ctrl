package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Semantic mapping proof for the canonical RuntimeSessionManager state vocabulary. */
public final class SessionUiStateTest {
    @Test
    public void mapsCanonicalSessionEventsWithoutInventingReadiness() {
        assertEquals(SessionUiState.Status.STARTING, SessionUiState.resolve("PTY_CREATED"));
        assertEquals(SessionUiState.Status.STARTING, SessionUiState.resolve("PTY_WAITING_FOR_PROMPT"));
        assertEquals(SessionUiState.Status.RUNNING, SessionUiState.resolve("READY"));
        assertEquals(SessionUiState.Status.RUNNING, SessionUiState.resolve("RUNNING"));
        assertEquals(SessionUiState.Status.FAILED, SessionUiState.resolve("BLOCKED"));
        assertEquals(SessionUiState.Status.FAILED, SessionUiState.resolve("BLOCKED_FGS_START"));
        assertEquals(SessionUiState.Status.FAILED, SessionUiState.resolve("STOPPING"));
        assertEquals(SessionUiState.Status.FINISHED, SessionUiState.resolve("FINISHED"));
        assertEquals(SessionUiState.Status.NOT_READY, SessionUiState.resolve("BACKGROUND_SESSION_PRESERVED"));
        assertEquals(SessionUiState.Status.NOT_READY, SessionUiState.resolve(null));
        assertEquals(SessionUiState.Status.NOT_READY, SessionUiState.resolve("UNKNOWN_STATE"));
    }
}
