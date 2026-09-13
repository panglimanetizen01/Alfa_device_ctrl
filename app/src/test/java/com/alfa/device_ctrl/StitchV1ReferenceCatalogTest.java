package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class StitchV1ReferenceCatalogTest {
    @Test public void catalogContainsEveryReferenceScreen() {
        assertEquals(159, StitchV1ReferenceCatalog.size());
        String[] ids = StitchV1ReferenceCatalog.screenIds();
        for (String id : ids) assertTrue("missing catalog id: " + id, StitchV1ReferenceCatalog.contains(id));
    }

    @Test public void canonicalBoundariesOverrideReferenceIdentityClaims() {
        assertEquals("com.alfa.device_ctrl", StitchV1ReferenceCatalog.CANONICAL_PACKAGE);
        assertEquals("/sdcard/Alfa_device_ctrl_HOST", StitchV1ReferenceCatalog.CANONICAL_HOST_ROOT);
        assertTrue(StitchV1ReferenceCatalog.ZIP_SHA256.matches("[0-9a-f]{64}"));
    }

    @Test public void highRiskReferenceStatesAreExplicitlyClassified() {
        assertEquals("security", StitchV1ReferenceCatalog.category("alfa_device_ctrl_security_diagnostic_console"));
        assertEquals("security", StitchV1ReferenceCatalog.category("alfa_device_ctrl_security_diagnostic_console_socket_rebind_success_fd_10"));
        assertEquals("split", StitchV1ReferenceCatalog.category("alfa_device_ctrl_split_pane_terminal_workspace_ubuntu_vs_kali"));
        assertEquals("floating", StitchV1ReferenceCatalog.category("alfa_device_ctrl_floating_terminal_overlay_permission_request"));
        assertEquals("settings", StitchV1ReferenceCatalog.category("alfa_device_ctrl_appearance_settings_datastore_proto_active_configuration"));
    }
}
