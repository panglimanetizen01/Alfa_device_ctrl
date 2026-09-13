package com.alfa.device_ctrl;

/** Tested clipboard boundary used by terminal copy/paste callbacks. */
public final class TerminalClipboardBridge {
    public interface ClipboardPort {
        void setText(String value);
        String getText();
    }

    public interface TerminalPort {
        void paste(String value);
    }

    private TerminalClipboardBridge() { }

    public static boolean copy(ClipboardPort clipboard, String text) {
        if (clipboard == null || text == null) return false;
        clipboard.setText(text);
        return true;
    }

    public static boolean paste(ClipboardPort clipboard, TerminalPort terminal) {
        if (clipboard == null || terminal == null) return false;
        String text = clipboard.getText();
        if (text == null) return false;
        terminal.paste(text);
        return true;
    }
}
