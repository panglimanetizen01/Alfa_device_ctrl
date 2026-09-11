package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;

/** Verifies that canonical settings survive a write/read cycle through Preferences DataStore. */
public final class AlfaSettingsStoreInstrumentationTest {
    @Test public void runtimeAndUiSettingsRoundTrip() {
        Context context=ApplicationProvider.getApplicationContext(); AlfaSettingsStore store=AlfaSettingsStore.get(context);
        store.setRuntimeId("debian");store.setLocale("ID_ID");store.setTerminalFont("monospace");store.setTerminalFontSize(12);store.setOverlayEnabled(true);store.setOverlayOpacityPercent(83);store.setOverlayWidthDp(520);store.setOverlayHeightDp(420);
        assertEquals("debian",store.getRuntimeId("missing"));assertEquals("ID_ID",store.getLocale("missing"));assertEquals("monospace",store.getTerminalFont("missing"));assertEquals(12,store.getTerminalFontSize(-1));assertTrue(store.getOverlayEnabled(false));assertEquals(83,store.getOverlayOpacityPercent(-1));assertEquals(520,store.getOverlayWidthDp(-1));assertEquals(420,store.getOverlayHeightDp(-1));
    }
}
