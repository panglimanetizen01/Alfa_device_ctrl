package com.alfa.device_ctrl;

import android.content.res.AssetManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Materializes the Gate 7 contract packaged in the exact APK into private runtime state. */
final class Gate7PackagedAssetProvisioner {
    static final String ASSET_NAME = "gate7-launch.properties";

    private Gate7PackagedAssetProvisioner() { }

    static boolean ensure(AssetManager assets, File launchFile, String runtimeId) {
        if (assets == null || launchFile == null || runtimeId == null) return false;
        try (InputStream in = assets.open(ASSET_NAME)) {
            byte[] packaged = readAll(in);
            if (matchesPackaged(launchFile, new ByteArrayInputStream(packaged), runtimeId)) return true;
            return provision(new ByteArrayInputStream(packaged), launchFile, runtimeId);
        } catch (Exception error) {
            return false;
        }
    }

    static boolean matchesPackaged(File launchFile, InputStream packagedAsset, String runtimeId) {
        if (launchFile == null || packagedAsset == null || runtimeId == null || !launchFile.isFile()) return false;
        try {
            if (!Gate6LaunchContract.verify(launchFile, runtimeId)) return false;
            byte[] packaged = readAll(packagedAsset);
            byte[] existing;
            try (InputStream in = new FileInputStream(launchFile)) { existing = readAll(in); }
            if (packaged.length != existing.length) return false;
            for (int i = 0; i < packaged.length; i++) {
                if (packaged[i] != existing[i]) return false;
            }
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    static boolean provision(InputStream packagedAsset, File launchFile, String runtimeId) {
        if (packagedAsset == null || launchFile == null || launchFile.getParentFile() == null || runtimeId == null) return false;
        File parent = launchFile.getParentFile();
        File temp = new File(parent, launchFile.getName() + ".packaged.partial");
        try {
            if (!parent.exists() && !parent.mkdirs()) return false;
            if (temp.exists() && !temp.delete()) return false;
            try (InputStream in = packagedAsset; OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            if (!Gate6LaunchContract.verify(temp, runtimeId)) {
                temp.delete();
                return false;
            }
            if (launchFile.exists() && !launchFile.delete()) {
                temp.delete();
                return false;
            }
            if (!temp.renameTo(launchFile)) {
                temp.delete();
                return false;
            }
            return Gate6LaunchContract.verify(launchFile, runtimeId);
        } catch (Exception error) {
            temp.delete();
            return false;
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        return out.toByteArray();
    }
}
