package com.termux.terminal;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A terminal session, consisting of a process coupled to a terminal interface. */
public final class TerminalSession extends TerminalOutput {
    private static final int MSG_NEW_INPUT = 1;
    private static final int MSG_PROCESS_EXITED = 4;

    public final String mHandle = UUID.randomUUID().toString();
    TerminalEmulator mEmulator;
    final ByteQueue mProcessToTerminalIOQueue = new ByteQueue(64 * 1024);
    final ByteQueue mTerminalToProcessIOQueue = new ByteQueue(4096);
    private final byte[] mUtf8InputBuffer = new byte[5];
    TerminalSessionClient mClient;
    int mShellPid;
    int mShellExitStatus;
    private int mTerminalFileDescriptor;
    public String mSessionName;
    final Handler mMainThreadHandler = new MainThreadHandler();
    private final String mShellPath;
    private final String mCwd;
    private final String[] mArgs;
    private final String[] mEnv;
    private final Integer mTranscriptRows;
    private static final String LOG_TAG = "TerminalSession";

    public TerminalSession(String shellPath, String cwd, String[] args, String[] env, Integer transcriptRows, TerminalSessionClient client) {
        this.mShellPath = shellPath;
        this.mCwd = cwd;
        this.mArgs = args;
        this.mEnv = env;
        this.mTranscriptRows = transcriptRows;
        this.mClient = client;
    }

    public void updateTerminalSessionClient(TerminalSessionClient client) {
        mClient = client;
        if (mEmulator != null) mEmulator.updateTerminalSessionClient(client);
    }

    public void updateSize(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        if (mEmulator == null) initializeEmulator(columns, rows, cellWidthPixels, cellHeightPixels);
        else {
            JNI.setPtyWindowSize(mTerminalFileDescriptor, rows, columns, cellWidthPixels, cellHeightPixels);
            mEmulator.resize(columns, rows, cellWidthPixels, cellHeightPixels);
        }
    }

    public String getTitle() { return (mEmulator == null) ? null : mEmulator.getTitle(); }

    public void initializeEmulator(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        mEmulator = new TerminalEmulator(this, columns, rows, cellWidthPixels, cellHeightPixels, mTranscriptRows, mClient);
        int[] processId = new int[1];
        mTerminalFileDescriptor = JNI.createSubprocess(mShellPath, mCwd, mArgs, mEnv, processId, rows, columns, cellWidthPixels, cellHeightPixels);
        mShellPid = processId[0];
        mClient.setTerminalShellPid(this, mShellPid);

        final FileDescriptor terminalFileDescriptorWrapped = wrapFileDescriptor(mTerminalFileDescriptor, mClient);
        new Thread("TermSessionInputReader[pid=" + mShellPid + "]") {
            @Override public void run() {
                try (InputStream termIn = new FileInputStream(terminalFileDescriptorWrapped)) {
                    final byte[] buffer = new byte[4096];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1) return;
                        if (!mProcessToTerminalIOQueue.write(buffer, 0, read)) return;
                        mMainThreadHandler.sendEmptyMessage(MSG_NEW_INPUT);
                    }
                } catch (Exception ignored) { }
            }
        }.start();

        new Thread("TermSessionOutputWriter[pid=" + mShellPid + "]") {
            @Override public void run() {
                final byte[] buffer = new byte[4096];
                try (FileOutputStream termOut = new FileOutputStream(terminalFileDescriptorWrapped)) {
                    while (true) {
                        int bytesToWrite = mTerminalToProcessIOQueue.read(buffer, true);
                        if (bytesToWrite == -1) return;
                        termOut.write(buffer, 0, bytesToWrite);
                    }
                } catch (IOException ignored) { }
            }
        }.start();

        new Thread("TermSessionWaiter[pid=" + mShellPid + "]") {
            @Override public void run() {
                int processExitCode = JNI.waitFor(mShellPid);
                mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(MSG_PROCESS_EXITED, processExitCode));
            }
        }.start();
    }

    /** Return the native shell PID, or 0 before creation and -1 after exit. */
    public int getPid() { return mShellPid; }

    @Override public void write(byte[] data, int offset, int count) {
        if (mShellPid > 0) mTerminalToProcessIOQueue.write(data, offset, count);
    }
    public void writeCodePoint(boolean prependEscape, int codePoint) {
        if (codePoint > 1114111 || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) throw new IllegalArgumentException("Invalid code point: " + codePoint);
        int bufferPosition = 0;
        if (prependEscape) mUtf8InputBuffer[bufferPosition++] = 27;
        if (codePoint < 128) mUtf8InputBuffer[bufferPosition++] = (byte) codePoint;
        else if (codePoint < 2048) {
            mUtf8InputBuffer[bufferPosition++] = (byte) (0xC0 | (codePoint >> 6));
            mUtf8InputBuffer[bufferPosition++] = (byte) (0x80 | (codePoint & 0x3F));
        } else if (codePoint < 65536) {
            mUtf8InputBuffer[bufferPosition++] = (byte) (0xE0 | (codePoint >> 12));
            mUtf8InputBuffer[bufferPosition++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
            mUtf8InputBuffer[bufferPosition++] = (byte) (0x80 | (codePoint & 0x3F));
        } else {
            mUtf8InputBuffer[bufferPosition++] = (byte) (0xF0 | (codePoint >> 18));
            mUtf8InputBuffer[bufferPosition++] = (byte) (0x80 | ((codePoint >> 12) & 0x3F));
            mUtf8InputBuffer[bufferPosition++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
            mUtf8InputBuffer[bufferPosition++] = (byte) (0x80 | (codePoint & 0x3F));
        }
        write(mUtf8InputBuffer, 0, bufferPosition);
    }

    public boolean isRunning() { return mShellPid > 0; }
    public TerminalEmulator getEmulator() { return mEmulator; }
    public int getExitStatus() { return mShellExitStatus; }
    public void finishIfRunning() { if (mShellPid > 0) JNI.sendSignal(mShellPid, OsConstants.SIGKILL); }

    private FileDescriptor wrapFileDescriptor(int fd, TerminalSessionClient client) {
        try {
            FileDescriptor fileDescriptor = new FileDescriptor();
            Field descriptorField = FileDescriptor.class.getDeclaredField("descriptor");
            descriptorField.setAccessible(true);
            descriptorField.setInt(fileDescriptor, fd);
            return fileDescriptor;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    final class MainThreadHandler extends Handler {
        @Override public void handleMessage(Message msg) {
            if (msg.what == MSG_NEW_INPUT) {
                if (mEmulator != null) mEmulator.readFromProcess();
                if (mClient != null) mClient.onTextChanged(TerminalSession.this);
            } else if (msg.what == MSG_PROCESS_EXITED) {
                mShellExitStatus = (Integer) msg.obj;
                mShellPid = -1;
                if (mClient != null) mClient.onSessionFinished(TerminalSession.this);
            }
        }
    }
}
