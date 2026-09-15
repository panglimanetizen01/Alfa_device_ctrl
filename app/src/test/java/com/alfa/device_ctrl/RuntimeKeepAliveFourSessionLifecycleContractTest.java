package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RuntimeKeepAliveFourSessionLifecycleContractTest {
    @Test
    public void fourRunningOwnersMustAllBeReattachedAfterActivityResume() {
        List<String> owners = Arrays.asList("debian", "ubuntu", "alpine", "kali");
        int reattached = owners.size() == 1 ? 1 : 0;
        assertEquals("all four runtime owners must be reattached", 4, reattached);
    }
}
