package com.termux.view;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Regression contract for the proven startup NPE: renderer metrics must exist before layout/session callbacks. */
public final class TerminalViewRendererInitializationContractTest {
    @Test public void constructorInitializesRendererBeforeGestureSetup() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/termux/view/TerminalView.java")), StandardCharsets.UTF_8);
        int constructor = source.indexOf("public TerminalView(Context context, AttributeSet attributes)");
        int renderer = source.indexOf("mRenderer = new TerminalRenderer(14, Typeface.MONOSPACE);", constructor);
        int gesture = source.indexOf("mGestureRecognizer = new GestureAndScaleRecognizer", constructor);
        assertTrue("TerminalView constructor missing", constructor >= 0);
        assertTrue("renderer must be initialized in constructor", renderer > constructor);
        assertTrue("renderer must initialize before gesture/layout state setup", renderer < gesture);
    }
}
