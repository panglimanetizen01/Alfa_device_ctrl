package com.alfa.device_ctrl;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RuntimeInstallerSymlinkSecurityTest {
    @Test
    public void absoluteSymlinkTargetIsRebasedInsideRootfs() throws Exception {
        File temp = Files.createTempDirectory("alfa-symlink-security-").toFile();
        File root = new File(temp, "rootfs");
        assertTrue(root.mkdirs());
        File archive = new File(temp, "rootfs.tar.gz");
        writeArchive(archive);

        RuntimeInstaller installer = new RuntimeInstaller(temp, null);
        Method extract = RuntimeInstaller.class.getDeclaredMethod("extractTar", File.class, File.class, boolean.class);
        extract.setAccessible(true);
        extract.invoke(installer, archive, root, true);

        Path link = new File(root, "bin/sh").toPath();
        assertTrue("symlink must be created", Files.isSymbolicLink(link));
        Path linkTarget = Files.readSymbolicLink(link);
        assertFalse("published symlink target must not be absolute", linkTarget.isAbsolute());
        Path resolved = link.getParent().resolve(linkTarget).normalize();
        assertTrue("resolved symlink target must remain inside rootfs", resolved.startsWith(root.toPath().toAbsolutePath().normalize()));
    }

    private static void writeArchive(File archive) throws Exception {
        try (OutputStream file = new FileOutputStream(archive); GZIPOutputStream gzip = new GZIPOutputStream(file)) {
            entry(gzip, "bin/", new byte[0], '5', "");
            entry(gzip, "usr/", new byte[0], '5', "");
            entry(gzip, "bin/sh", new byte[0], '2', "/usr/bin/env");
            entry(gzip, "usr/bin/env", "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8), '0', "");
            gzip.write(new byte[1024]);
        }
    }

    private static void entry(OutputStream out, String name, byte[] body, char type, String link) throws Exception {
        byte[] header = new byte[512];
        put(header, 0, 100, name);
        put(header, 100, 8, "0000755");
        put(header, 124, 12, String.format("%011o", body.length));
        header[156] = (byte) type;
        put(header, 157, 100, link);
        put(header, 257, 6, "ustar");
        for (int i = 148; i < 156; i++) header[i] = ' ';
        long sum = 0;
        for (byte value : header) sum += value & 0xff;
        put(header, 148, 8, String.format("%06o", sum));
        out.write(header);
        out.write(body);
        int padding = (int) ((512 - (body.length % 512)) % 512);
        if (padding > 0) out.write(new byte[padding]);
    }

    private static void put(byte[] target, int offset, int width, String value) {
        byte[] bytes = (value + "\0").getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, target, offset, Math.min(value.length(), width));
    }
}
