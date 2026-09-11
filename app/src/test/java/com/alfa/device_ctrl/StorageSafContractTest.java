package com.alfa.device_ctrl;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public final class StorageSafContractTest {
    private static final Path ROOT = Paths.get("src/main/java/com/alfa/device_ctrl");

    private static String readSource(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test public void pickerUsesRealOpenDocumentTreeAndPersistsUriGrant() throws Exception {
        String source = readSource(ROOT.resolve("StoragePickerActivity.java"));
        assertTrue(source.contains("Intent.ACTION_OPEN_DOCUMENT_TREE"));
        assertTrue(source.contains("FLAG_GRANT_PERSISTABLE_URI_PERMISSION"));
        assertTrue(source.contains("takePersistableUriPermission"));
        assertTrue(source.contains("getSharedPreferences(\"alfa_storage\""));
    }

    @Test public void explorerConsumesPersistedSafTree() throws Exception {
        String source = readSource(ROOT.resolve("StorageUiBridge.java"));
        assertTrue(source.contains("DocumentFile.fromTreeUri"));
        assertTrue(source.contains("getSharedPreferences(\"alfa_storage\""));
        assertTrue(source.contains("tree.listFiles()"));
        assertTrue(source.contains("StoragePickerActivity.class"));
    }
}
