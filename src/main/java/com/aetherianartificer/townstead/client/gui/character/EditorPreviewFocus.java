package com.aetherianartificer.townstead.client.gui.character;

import net.minecraft.Util;

/**
 * Camera focus for the villager editor's preview model. While the user drags an
 * attachment slider (or cycles a style / picks a colour), the editor mixin points
 * this at the affected body region — the preview render then zooms toward it and
 * eases back to the full figure shortly after the last interaction. {@code height}
 * is the model-height fraction centered in the view (0 = feet, 1 = head top);
 * neutral is 0.5 / zoom 1, exactly the vanilla framing, so an idle editor renders
 * byte-identically to an unmixed one.
 */
public final class EditorPreviewFocus {

    private static final long HOLD_MS = 700;

    private static float targetHeight = 0.5f, targetZoom = 1f;
    private static float height = 0.5f, zoom = 1f;
    private static long activeUntil;
    private static long lastMs;

    private EditorPreviewFocus() {}

    /** Aim the preview at a body region for a short while (refreshed on every drag tick). */
    public static void focus(float heightFraction, float zoomFactor) {
        targetHeight = heightFraction;
        targetZoom = zoomFactor;
        activeUntil = Util.getMillis() + HOLD_MS;
    }

    /** Drop back to the neutral framing immediately (screen closed). */
    public static void clear() {
        activeUntil = 0;
        height = 0.5f;
        zoom = 1f;
    }

    /** The eased zoom multiplier for this frame (1 = vanilla framing). */
    public static float zoomNow() {
        tick();
        return zoom;
    }

    /** The eased centered model-height fraction for this frame (0.5 = vanilla framing). */
    public static float heightNow() {
        tick();
        return height;
    }

    private static void tick() {
        long now = Util.getMillis();
        float dt = lastMs == 0 ? 0f : Math.min(0.2f, (now - lastMs) / 1000f);
        lastMs = now;
        float toHeight = now < activeUntil ? targetHeight : 0.5f;
        float toZoom = now < activeUntil ? targetZoom : 1f;
        float alpha = 1f - (float) Math.exp(-10.0 * dt);
        height += (toHeight - height) * alpha;
        zoom += (toZoom - zoom) * alpha;
    }
}
