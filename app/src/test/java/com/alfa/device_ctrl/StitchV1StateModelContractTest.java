package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Contract for deterministic native implementation of all supplied Stitch v1 states. */
public final class StitchV1StateModelContractTest {
    @Test
    public void everyCatalogStateHasDeterministicNativeMappingAndAction() {
        String[] ids = StitchV1ReferenceCatalog.screenIds();
        assertEquals(159, ids.length);
        for (String id : ids) {
            StitchV1StateModel.State state = StitchV1StateModel.resolve(id);
            assertNotNull(id, state);
            assertFalse(id, state.domain().isEmpty());
            assertNotNull(id, state.primaryScreen());
            assertNotNull(id, state.action());
            assertTrue(id, state.supportedActionCount() > 0);
        }
    }

    @Test
    public void simulationAndKnownResultStatesNeverBecomeExecutableClaims() {
        for (String id : StitchV1ReferenceCatalog.screenIds()) {
            StitchV1StateModel.State state = StitchV1StateModel.resolve(id);
            if (id.contains("simulation") || id.contains("successfully_restored_applied") ||
                    id.contains("webhook_event_dispatched_success") || id.contains("terminated_via_sigterm") ||
                    id.contains("apt_upgrade_y_completed")) {
                assertTrue(id, state.referenceOnly());
                assertFalse(id, state.claimsLiveExecution());
                assertNull(id, state.runtimeCommand());
            }
        }
    }

    @Test
    public void executableCatalogStatesUseOnlyExplicitRuntimeCommands() {
        assertEquals("apt update", StitchV1StateModel.resolve("alfa_device_ctrl_terminal_pts_5_apt_update_executed_in_ubuntu").runtimeCommand());
        assertEquals("clear", StitchV1StateModel.resolve("alfa_device_ctrl_terminal_pts_5_screen_cleared_via_clear_command").runtimeCommand());
        assertEquals("uname -a && ls -la", StitchV1StateModel.resolve("alfa_device_ctrl_floating_terminal_uname_a_ls_la_executed").runtimeCommand());
        assertEquals(StitchV1StateModel.Action.CONFIRM_STOP_SESSION,
                StitchV1StateModel.resolve("alfa_device_ctrl_stop_session_5_confirmation_dialog_ubuntu_pts_4_1").action());
    }

    @Test
    public void stateDomainsCoverTheOperationalSurface() {
        assertEquals("terminal", StitchV1StateModel.resolve("alfa_device_ctrl_operational_terminal_runtime_dashboard").domain());
        assertEquals("security", StitchV1StateModel.resolve("alfa_device_ctrl_security_diagnostic_console").domain());
        assertEquals("storage-policy", StitchV1StateModel.resolve("alfa_device_ctrl_mount_isolation_policy_settings").domain());
        assertEquals("network", StitchV1StateModel.resolve("alfa_device_ctrl_matrix_ops_bot_webhook_notification_dispatch").domain());
        assertEquals("settings", StitchV1StateModel.resolve("alfa_device_ctrl_appearance_settings_high_contrast_aaa_preset_active").domain());
        assertEquals("sessions", StitchV1StateModel.resolve("alfa_device_ctrl_session_manager_spawn_new_terminal_session").domain());
        assertEquals("split", StitchV1StateModel.resolve("alfa_device_ctrl_split_pane_terminal_vertical_split_vert_ubuntu_focused").domain());
        assertEquals("floating", StitchV1StateModel.resolve("alfa_device_ctrl_floating_terminal_minimized_bubble_mode").domain());
    }
}
