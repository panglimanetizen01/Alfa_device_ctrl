package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Production source must not inherit stale HTML/reference-only package, storage, or web-runtime contracts. */
public final class UploadedUiReferenceLeakTest {
    @Test
    public void productionJavaSourceDoesNotContainStaleUiReferenceContracts() throws Exception {
        File root = new File("src/main/java");
        List<File> files = new ArrayList<>();
        collect(root, files);
        for (File file : files) {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            assertFalse("stale package leaked into production source: " + file, text.contains("moe.alfa.device.ctrl"));
            assertFalse("stale host storage path leaked into production source: " + file,
                    text.contains("/storage/emulated/0/Android/data/moe.alfa.device.ctrl"));
            assertFalse("HTML runtime dependency leaked into production source: " + file,
                    text.contains("cdn.tailwindcss.com"));
            assertFalse("browser mock runtime API leaked into production source: " + file,
                    text.contains("Math.random()"));
        }
    }

    private static void collect(File directory, List<File> output) {
        if (directory == null || !directory.isDirectory()) return;
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collect(child, output);
            else if (child.getName().endsWith(".java")) output.add(child);
        }
    }
}
