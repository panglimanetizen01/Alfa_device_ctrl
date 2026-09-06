package com.alfa.device_ctrl;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalViewClient;

/** Minimal host adapter for the upstream Apache-2.0 TerminalView library. */
public final class AlfaTerminalViewClient implements TerminalViewClient {
    @Override public float onScale(float scale) { return scale; }
    @Override public void onSingleTapUp(MotionEvent event) { }
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
