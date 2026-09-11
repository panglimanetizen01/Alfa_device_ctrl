package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Environment;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

/**
 * Evidence gate for milestones 15-20.
 *
 * This class intentionally verifies only observable platform/UI contracts. It does not
 * manufacture a runtime, Shizuku identity, Termux callback, or visual baseline. Those
 * require the physical DUT and are therefore gated separately by the release checklist.
 */
@RunWith(AndroidJUnit4.class)
public final class UiMilestone15To20InstrumentationTest {
    private static final long WAIT_MS = 8_000L;
    private Context targetContext;
    private UiDevice device;

    @Before
    public void setUp() {
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void launch_relaunch_and_rotation_keep_app_observable() throws Exception {
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            assertTrue("MainActivity must reach foreground", device.wait(Until.hasObject(By.pkg(targetContext.getPackageName())), WAIT_MS));
            assertEquals(targetContext.getPackageName(), device.getCurrentPackageName());

            scenario.onActivity(Activity::recreate);
            assertTrue("MainActivity must survive Activity recreation", device.wait(Until.hasObject(By.pkg(targetContext.getPackageName())), WAIT_MS));
            assertEquals(targetContext.getPackageName(), device.getCurrentPackageName());
        }
    }

    @Test
    public void clickable_targets_are_labeled_and_have_minimum_touch_bounds() throws Exception {
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            assertTrue(device.wait(Until.hasObject(By.pkg(targetContext.getPackageName())), WAIT_MS));

            List<UiObject2> clickable = device.findObjects(By.pkg(targetContext.getPackageName()).clickable(true));
            assertFalse("MainActivity must expose at least one interactive control", clickable.isEmpty());

            float density = targetContext.getResources().getDisplayMetrics().density;
            int minimumPx = (int) Math.ceil(48f * density);
            int checked = 0;
            for (UiObject2 node : clickable) {
                Rect bounds = node.getVisibleBounds();
                String text = node.getText();
                String description = node.getContentDescription();
                boolean labeled = (text != null && !text.trim().isEmpty())
                        || (description != null && !description.toString().trim().isEmpty());
                assertTrue("Clickable UI element must have text or contentDescription: " + bounds, labeled);
                assertTrue("Clickable UI element is narrower than 48dp: " + bounds,
                        bounds.width() >= minimumPx);
                assertTrue("Clickable UI element is shorter than 48dp: " + bounds,
                        bounds.height() >= minimumPx);
                checked++;
            }
            assertTrue("No clickable controls were actually checked", checked > 0);
        }
    }

    @Test
    public void package_manifest_and_permission_contract_are_resolvable() throws Exception {
        PackageManager pm = targetContext.getPackageManager();
        PackageInfo info = pm.getPackageInfo(targetContext.getPackageName(), PackageManager.GET_PERMISSIONS);
        assertNotNull(info);
        assertNotNull(info.applicationInfo);

        Intent launcher = pm.getLaunchIntentForPackage(targetContext.getPackageName());
        assertNotNull("Application must have a launcher activity", launcher);
        assertNotNull("Application must declare package metadata", info.packageName);

        if (info.requestedPermissions != null) {
            for (String permission : info.requestedPermissions) {
                assertNotNull("Manifest permission name must not be null", permission);
                assertFalse("Manifest permission name must not be empty", permission.trim().isEmpty());
            }
        }
    }

    @Test
    public void screenshot_evidence_is_captured_without_claiming_visual_match() throws Exception {
        Intent intent = new Intent(targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            assertTrue(device.wait(Until.hasObject(By.pkg(targetContext.getPackageName())), WAIT_MS));
            File evidenceDir = new File(targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "alfa-ui-milestone-15-20");
            assertTrue("Evidence directory must be creatable", evidenceDir.mkdirs() || evidenceDir.isDirectory());
            File screenshot = new File(evidenceDir, "main.png");
            assertTrue("Screenshot capture failed", device.takeScreenshot(screenshot));
            assertTrue("Screenshot evidence is empty", screenshot.isFile() && screenshot.length() > 0);
        }
    }
}
