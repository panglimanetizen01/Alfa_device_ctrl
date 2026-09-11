package com.alfa.device_ctrl;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Map;
import java.util.WeakHashMap;

/** Reallocates the existing runtime/monitor/terminal panes from current window height. */
public final class AdaptivePanelLayoutController {
    private static final Map<LinearLayout, View[]> PANELS = new WeakHashMap<>();

    private AdaptivePanelLayoutController() { }

    public static void install(View contentRoot, ActualWindowMetrics metrics) {
        if (!(contentRoot instanceof ViewGroup)) return;
        LinearLayout content = findAdaptiveContent((ViewGroup) contentRoot);
        if (content == null) return;
        View[] panels = PANELS.get(content);
        if (panels == null) {
            panels = discoverPanels(content);
            if (panels == null) return;
            PANELS.put(content, panels);
        }
        apply(content, panels, metrics);
    }

    static void apply(LinearLayout content, View[] panels, ActualWindowMetrics metrics) {
        float runtimeWeight = AdaptivePanelPolicy.runtimeWeight(metrics.widthClass(), metrics.heightClass());
        float monitorWeight = AdaptivePanelPolicy.monitorWeight(metrics.widthClass(), metrics.heightClass());
        setWeightedHeight(panels[0], runtimeWeight);
        setWeightedHeight(panels[1], monitorWeight);
        setWeightedHeight(panels[2], 1f);
        content.requestLayout();
    }

    private static void setWeightedHeight(View view, float weight) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (!(raw instanceof LinearLayout.LayoutParams)) return;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) raw;
        params.height = 0;
        params.weight = weight;
        view.setLayoutParams(params);
    }

    private static LinearLayout findAdaptiveContent(ViewGroup root) {
        if (root instanceof LinearLayout) {
            LinearLayout candidate = (LinearLayout) root;
            if (candidate.getOrientation() == LinearLayout.VERTICAL && candidate.getChildCount() >= 5) return candidate;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                LinearLayout found = findAdaptiveContent((ViewGroup) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static View[] discoverPanels(LinearLayout content) {
        View[] fixed = new View[2];
        int found = 0;
        View terminal = null;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            ViewGroup.LayoutParams raw = child.getLayoutParams();
            if (!(raw instanceof LinearLayout.LayoutParams)) continue;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) raw;
            if (params.weight > 0f) {
                if (terminal == null) terminal = child;
                continue;
            }
            if (params.height > 100) {
                if (found < fixed.length) fixed[found++] = child;
            }
        }
        if (found != 2 || terminal == null) return null;
        return new View[]{fixed[0], fixed[1], terminal};
    }
}
