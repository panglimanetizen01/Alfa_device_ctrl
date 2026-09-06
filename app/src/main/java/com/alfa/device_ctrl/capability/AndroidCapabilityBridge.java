package com.alfa.device_ctrl.capability;

import android.content.Context;
import android.net.Uri;

public interface AndroidCapabilityBridge {
    SafAndroidCapabilityBridge.Result createWriteRead(
            Context context,
            Uri treeUri,
            String displayName,
            String mimeType,
            String payload);
}
