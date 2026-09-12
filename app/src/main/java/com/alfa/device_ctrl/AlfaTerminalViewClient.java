package com.alfa.device_ctrl;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

/** Host adapter for TerminalView with an explicit native Android IME boundary. */
public final class AlfaTerminalViewClient implements TerminalViewClient {
    public interface SoftKeyboardRequester { void show(View target); }

    private View terminalView;
    private SoftKeyboardRequester softKeyboardRequester;

    public AlfaTerminalViewClient() { }

    public AlfaTerminalViewClient(View terminalView, SoftKeyboardRequester requester) {
        bind(terminalView, requester);
    }

    public void bind(View terminalView, SoftKeyboardRequester requester) {
        this.terminalView = terminalView;
        this.softKeyboardRequester = requester;
    }

    @Override public float onScale(float scale) {
        if (terminalView instanceof TerminalView) {
            float factor = Math.max(0.75f, Math.min(1.75f, scale));
            TerminalView view = (TerminalView) terminalView;
            int size = Math.max(9, Math.min(22, Math.round(13f * factor)));
            view.setTextSize(size);
        }
        return scale;
    }

    @Override public void onSingleTapUp(MotionEvent event) {
        if (terminalView == null) return;
        terminalView.setFocusableInTouchMode(true);
        terminalView.requestFocus();
        if (softKeyboardRequester != null) softKeyboardRequester.show(terminalView);
    }

    @Override public boolean shouldBackButtonBeMappedToEscape() { return true; }
    @Override public boolean shouldEnforceCharBasedInput() { return true; }
    @Override public boolean shouldUseCtrlSpaceWorkaround() { return false; }
    @Override public boolean isTerminalViewSelected() { return true; }
    @Override public void copyModeChanged(boolean copyMode) { }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event, TerminalSession session) { return false; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) { return false; }
    @Override public boolean onLongPress(MotionEvent event) { return false; }
    @Override public boolean readControlKey() { return false; }
    @Override public boolean readAltKey() { return false; }
    @Override public boolean readShiftKey() { return false; }
    @Override public boolean readFnKey() { return false; }
    @Override public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) { return false; }
    @Override public void onEmulatorSet() { }
    @Override public void logError(String tag, String message) { }
    @Override public void logWarn(String tag, String message) { }
    @Override public void logInfo(String tag, String message) { }
    @Override public void logDebug(String tag, String message) { }
    @Override public void logVerbose(String tag, String message) { }
    @Override public void logStackTraceWithMessage(String tag, String message, Exception e) { }
    @Override public void logStackTrace(String tag, Exception e) { }
}
