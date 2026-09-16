package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class RuntimeAbiTest {
    @Test public void selectsFirstSupportedNativeAbiInCanonicalOrder() {
        assertEquals("x86_64", RuntimeAbi.select(new String[]{"x86_64", "arm64-v8a"}));
        assertEquals("arm64-v8a", RuntimeAbi.select(new String[]{"arm64-v8a", "x86_64"}));
    }

    @Test public void rejectsUnsupportedAbiInsteadOfFallingBackToArm64() {
        assertThrows(IllegalArgumentException.class,
                () -> RuntimeAbi.select(new String[]{"armeabi-v7a", "x86"}));
    }

    @Test public void mapsNativeAbiToAndroidJniDirectory() {
        assertEquals("arm64-v8a", RuntimeAbi.jniDirectory("arm64-v8a"));
        assertEquals("x86_64", RuntimeAbi.jniDirectory("x86_64"));
    }

    @Test public void rejectsNullAndEmptyAbiLists() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeAbi.select(null));
        assertThrows(IllegalArgumentException.class, () -> RuntimeAbi.select(new String[0]));
    }
}
