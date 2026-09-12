package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TerminalClipboardBridgeContractTest {
    @Test public void copyPublishesSelectedTerminalTextToClipboardPort() {
        FakeClipboard clipboard = new FakeClipboard();
        assertTrue(TerminalClipboardBridge.copy(clipboard, "ALFA_TEST\nline-2"));
        assertEquals("ALFA_TEST\nline-2", clipboard.text);
    }

    @Test public void pasteReadsClipboardTextAndDeliversItToTerminal() {
        FakeClipboard clipboard = new FakeClipboard();
        clipboard.text = "echo PASTED";
        FakeTerminal terminal = new FakeTerminal();
        assertTrue(TerminalClipboardBridge.paste(clipboard, terminal));
        assertEquals("echo PASTED", terminal.text);
    }

    @Test public void copyAndPasteFailClosedForMissingText() {
        FakeClipboard clipboard = new FakeClipboard();
        FakeTerminal terminal = new FakeTerminal();
        assertTrue(!TerminalClipboardBridge.copy(clipboard, null));
        assertTrue(!TerminalClipboardBridge.paste(clipboard, terminal));
        assertEquals(null, terminal.text);
    }

    private static final class FakeClipboard implements TerminalClipboardBridge.ClipboardPort {
        String text;
        @Override public void setText(String value) { text = value; }
        @Override public String getText() { return text; }
    }

    private static final class FakeTerminal implements TerminalClipboardBridge.TerminalPort {
        String text;
        @Override public void paste(String value) { text = value; }
    }
}
