package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Test;

public final class StitchV1ArtifactBoundaryTest {
    private static final String EXPECTED_SHA256 = "7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d";
    private static final File ARTIFACT = new File("../../stitch/stitch_alfa_device_control_v1.0.0.zip");

    @Test public void canonicalZipIsPresentAndHasExpectedHash() throws Exception {
        assertTrue("canonical Stitch ZIP must be tracked in the repository", ARTIFACT.isFile());
        assertEquals(EXPECTED_SHA256, sha256(ARTIFACT));
    }

    @Test public void canonicalZipAssetCountsMatchForensicBaseline() throws Exception {
        assertTrue("canonical Stitch ZIP must be tracked in the repository", ARTIFACT.isFile());
        int html = 0;
        int png = 0;
        int markdown = 0;
        int files = 0;
        try (ZipFile zip = new ZipFile(ARTIFACT)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                files++;
                String name = entry.getName().toLowerCase();
                if (name.endsWith("code.html")) html++;
                if (name.endsWith(".png")) png++;
                if (name.endsWith(".md") || name.endsWith(".markdown")) markdown++;
            }
        }
        assertEquals("159 Stitch code.html states", 159, html);
        assertEquals("160 Stitch PNG assets", 160, png);
        assertEquals("7 Stitch markdown files", 7, markdown);
        assertEquals("326 tracked files in the supplied Stitch corpus", 326, files);
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int read;
        try (FileInputStream in = new FileInputStream(file)) {
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format("%02x", b));
        return out.toString();
    }
}
