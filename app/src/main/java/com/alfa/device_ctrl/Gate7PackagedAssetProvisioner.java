package com.alfa.device_ctrl;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Materializes the Gate 7 contract packaged in the exact APK into private runtime state. */
final class Gate7PackagedAssetProvisioner {
    static final String ASSET_NAME = "gate7-launch.properties";

    private Gate7PackagedAssetProvisioner() { }

    static boolean ensure(AssetManager assets, File launchFile, String runtimeId) {
        if (assets == null || launchFile == null || runtimeId == null) return false;
        if (Gate6LaunchContract.verify(launchFile, runtimeId)) return true;
        try (InputStream in = assets.open(ASSET_NAME)) {
            return provision(in, launchFile, runtimeId);
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
}
