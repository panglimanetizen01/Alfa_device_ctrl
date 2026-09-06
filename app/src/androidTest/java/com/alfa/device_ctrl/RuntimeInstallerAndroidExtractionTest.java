package com.alfa.device_ctrl;

import org.junit.Test;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public final class RuntimeInstallerAndroidExtractionTest {

    @Test
    public void productionExtractorMaterializesRealUbuntuArchive() throws Exception {
        File archive = new File(
        getInstrumentation().getTargetContext().getFilesDir(),
        "alfa-ubuntu-base-24.04.4.tar.gz");
        assertTrue("ANDROID_ARCHIVE_MISSING=" + archive, archive.isFile());

        File temp = Files.createTempDirectory("alfa-android-extract-").toFile();
        File root = new File(temp, "rootfs");
        assertTrue(root.mkdirs());

        File localArchive = new File(temp, "ubuntu-base.tar.gz");
        copy(archive, localArchive);

        RuntimeInstaller installer = new RuntimeInstaller(temp, null);

        Method extractTar = RuntimeInstaller.class.getDeclaredMethod(
                "extractTar",
                File.class,
                File.class,
                boolean.class);
        extractTar.setAccessible(true);

        extractTar.invoke(installer, localArchive, root, true);

        File bin = new File(root, "bin");
        File usrBin = new File(root, "usr/bin");
        File env = new File(root, "usr/bin/env");
        File dash = new File(root, "usr/bin/dash");
        File bash = new File(root, "usr/bin/bash");
        File loader = new File(root, "usr/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1");

        System.out.println("===== ANDROID PRODUCTION EXTRACTOR RESULT =====");
        report("ROOTFS", root);
        report("BIN", bin);
        report("USR_BIN", usrBin);
        report("ENV", env);
        report("DASH", dash);
        report("BASH", bash);
        report("LOADER", loader);

        System.out.println("TOP_LEVEL_ENTRIES=");
        File[] top = root.listFiles();
        if (top != null) {
            for (File f : top) {
                System.out.println("  " + f.getName() + " type=" +
                        (f.isDirectory() ? "DIR" : f.isFile() ? "FILE" : "OTHER"));
            }
        }

        System.out.println("USR_BIN_SAMPLE=");
        File[] entries = usrBin.listFiles();
        if (entries != null) {
            int count = 0;
            for (File f : entries) {
                System.out.println("  " + f.getName() + " type=" +
                        (f.isDirectory() ? "DIR" : f.isFile() ? "FILE" : "OTHER") +
                        " size=" + f.length());
                if (++count >= 30) break;
            }
        }

        assertTrue("extractor must materialize /usr/bin/env", env.isFile());
        assertTrue("extractor must materialize /usr/bin/dash", dash.exists());
        assertTrue("extractor must materialize /usr/bin/bash", bash.exists());
        assertTrue("extractor must materialize dynamic loader", loader.exists());
    }

    private static void copy(File source, File destination) throws Exception {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static void report(String name, File file) {
        System.out.println(
                name + "=" + file +
                " exists=" + file.exists() +
                " file=" + file.isFile() +
                " dir=" + file.isDirectory() +
                " size=" + file.length() +
                " readable=" + file.canRead() +
                " writable=" + file.canWrite() +
                " executable=" + file.canExecute());
    }
}
