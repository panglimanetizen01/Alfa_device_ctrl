package com.termux.view;

import static org.junit.Assert.assertEquals;

import android.graphics.Typeface;

import org.junit.Test;

public final class TerminalRendererLineSpacingTest {
    @Test public void lineSpacingMultiplierChangesMeasuredLineHeight() {
        TerminalRenderer renderer = new TerminalRenderer(13, Typeface.MONOSPACE, 1.25f);
        TerminalRenderer defaultRenderer = new TerminalRenderer(13, Typeface.MONOSPACE, 1.0f);
        assertEquals(Math.round(defaultRenderer.mFontLineSpacing * 1.25f), renderer.mFontLineSpacing);
    }
}
