package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class ExternalExecutionEvidenceStoreContractTest {
    @Test
    public void coordinatorPersistsResultsToExternalEvidenceStore() throws Exception {
        File coordinator = new File("src/main/java/com/alfa/device_ctrl/external/ExternalExecutionCoordinator.java");
        File store = new File("src/main/java/com/alfa/device_ctrl/external/ExternalExecutionEvidenceStore.java");
        String coordinatorText = new String(Files.readAllBytes(coordinator.toPath()), StandardCharsets.UTF_8);
        String storeText = new String(Files.readAllBytes(store.toPath()), StandardCharsets.UTF_8);
        assertTrue(coordinatorText.contains("ExternalExecutionEvidenceStore.persist"));
        assertTrue(storeText.contains("external-execution-v1"));
        assertTrue(storeText.contains("pid="));
        assertTrue(storeText.contains("exit_code="));
        assertTrue(storeText.contains("stdout="));
        assertTrue(storeText.contains("stderr="));
    }
}
