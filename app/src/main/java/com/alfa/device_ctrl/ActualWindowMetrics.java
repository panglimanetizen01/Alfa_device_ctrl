package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

/** Immutable snapshot of the window actually available to an Activity. */
public final class ActualWindowMetrics {
    private final int widthPx;
    private final int heightPx;
    private final int widthDp;
    private final int heightDp;

    private ActualWindowMetrics(int widthPx, int heightPx, float density) {
        this.widthPx = Math.max(0, widthPx);
        this.heightPx = Math.max(0, heightPx);
        this.widthDp = Math.max(0, Math.round(this.widthPx / Math.max(density, 0.01f)));
        this.heightDp = Math.max(0, Math.round(this.heightPx / Math.max(density, 0.01f)));
    }

    public static ActualWindowMetrics from(Activity activity) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        int widthPx;
        int heightPx;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            widthPx = bounds.width();
            heightPx = bounds.height();
        } else {
            Display display = activity.getWindowManager().getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);
            widthPx = size.x;
            heightPx = size.y;
        }
        return new ActualWindowMetrics(widthPx, heightPx, displayMetrics.density);
    }

    public int widthPx() { return widthPx; }
    public int heightPx() { return heightPx; }
    public int widthDp() { return widthDp; }
    public int heightDp() { return heightDp; }
    public AdaptiveWindowPolicy.WidthClass widthClass() { return AdaptiveWindowPolicy.widthClass(widthDp); }
    public AdaptiveWindowPolicy.HeightClass heightClass() { return AdaptiveWindowPolicy.heightClass(heightDp); }
    public boolean useTwoPane() { return AdaptiveWindowPolicy.useTwoPane(widthDp); }
    public boolean usePersistentNavigation() { return AdaptiveWindowPolicy.usePersistentNavigation(widthDp); }
}
