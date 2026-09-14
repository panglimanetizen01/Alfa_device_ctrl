package com.alfa.device_ctrl;

import java.util.Arrays;
import java.util.List;

/**
 * Retired legacy presentation hook.
 *
 * The canonical Stitch UI is owned by MainActivity plus AlfaFinalUiHooks.
 * This compatibility type is intentionally inert so stale lifecycle wiring
 * cannot reintroduce the retired presentation surface. Its token constants
 * remain only for existing forensic contract tests and are not rendered.
 */
@Deprecated
public final class AlfaFinalUiPresentation {
    public static final int CYAN = 0xFF38BDF8;
    public static final int TERMINAL = 0xFF0D1117;
    public static final List<Integer> FONT_SIZES_SP = Arrays.asList(11, 13, 15);
    public static final List<Float> LINE_HEIGHTS = Arrays.asList(1.0f, 1.25f, 1.5f);

    private AlfaFinalUiPresentation() {
        throw new AssertionError("retired-ui-presenter");
    }
}
