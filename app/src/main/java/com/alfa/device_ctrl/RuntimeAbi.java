package com.alfa.device_ctrl;

import java.util.Locale;

/** Canonical native-runtime ABI selection. Fail closed; never silently fall back across ABIs. */
public final class RuntimeAbi {
    public static final String ARM64_V8A = "arm64-v8a";
    public static final String X86_64 = "x86_64";

    private RuntimeAbi() { }

    public static String select(String[] supportedAbis) {
        if (supportedAbis == null || supportedAbis.length == 0) {
            throw new IllegalArgumentException("no-supported-native-abi");
        }
        for (String abi : supportedAbis) {
            if (abi == null) continue;
            String normalized = abi.trim().toLowerCase(Locale.ROOT);
            if (ARM64_V8A.equals(normalized) || X86_64.equals(normalized)) return normalized;
        }
        throw new IllegalArgumentException("unsupported-native-abi:" + supportedAbis[0]);
    }

    public static String jniDirectory(String abi) {
        if (ARM64_V8A.equals(abi) || X86_64.equals(abi)) return abi;
        throw new IllegalArgumentException("unsupported-native-abi:" + abi);
    }
}
