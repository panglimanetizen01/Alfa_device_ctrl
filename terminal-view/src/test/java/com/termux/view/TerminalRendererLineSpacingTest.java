package com.termux.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TerminalRendererLineSpacingTest {
    @Test public void lineSpacingMultiplierChangesMeasuredLineHeight() {
        int base = TerminalRenderer.calculateLineSpacing(15f, 1.0f);
        int expanded = TerminalRenderer.calculateLineSpacing(15f, 1.25f);
        int clamped = TerminalRenderer.calculateLineSpacing(15f, 4.0f);
        assertEquals(15, base);
        assertEquals(19, expanded);
        assertEquals(27, clamped);
    }
}
