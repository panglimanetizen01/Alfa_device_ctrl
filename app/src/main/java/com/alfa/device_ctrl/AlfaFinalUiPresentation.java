package com.alfa.device_ctrl;

/**
 * Retired legacy presentation hook.
 *
 * The canonical Stitch UI is owned by MainActivity plus AlfaFinalUiHooks.
 * This compatibility type is intentionally inert so stale lifecycle wiring
 * cannot reintroduce the retired presentation surface.
 */
@Deprecated
public final class AlfaFinalUiPresentation {
    private AlfaFinalUiPresentation() {
        throw new AssertionError("retired-ui-presenter");
    }
}
