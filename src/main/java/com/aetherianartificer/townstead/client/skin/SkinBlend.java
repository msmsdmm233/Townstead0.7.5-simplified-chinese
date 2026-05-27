package com.aetherianartificer.townstead.client.skin;

/**
 * The skin-tint blend math, shared by the skin-layer mixin (recolours the rendered villager)
 * and the Body picker texture (recolours the gradient square) so both stay identical. Blend
 * ordinals: {@code 0} multiply (darken, white = identity), {@code 1} screen (lighten,
 * black = identity), {@code 2} overlay (both, mid-grey = identity).
 */
public final class SkinBlend {

    private SkinBlend() {}

    /** Blend one 0–255 channel of {@code base} by {@code tint} under the given mode. */
    public static int channel(int base, int tint, int mode) {
        switch (mode) {
            case 1:  return 255 - (255 - base) * (255 - tint) / 255;
            case 2:  return base < 128 ? 2 * base * tint / 255 : 255 - 2 * (255 - base) * (255 - tint) / 255;
            default: return base * tint / 255;
        }
    }

    /** Blend a packed 0xRRGGBB base by a packed 0xRRGGBB tint under the given mode. */
    public static int rgb(int base, int tint, int mode) {
        int r = channel((base >> 16) & 0xFF, (tint >> 16) & 0xFF, mode);
        int g = channel((base >> 8) & 0xFF, (tint >> 8) & 0xFF, mode);
        int b = channel(base & 0xFF, tint & 0xFF, mode);
        return (r << 16) | (g << 8) | b;
    }

    /** The no-op tint for a mode: white (multiply), black (screen), mid-grey (overlay). */
    public static int identity(int mode) {
        switch (mode) {
            case 1:  return 0x000000;
            case 2:  return 0x808080;
            default: return 0xFFFFFF;
        }
    }

    /**
     * Scale a tint's effect by {@code strength} (0–1). Every blend is linear in the tint channel,
     * so lerping the tint toward the mode's identity is exactly equivalent to lerping the blended
     * result toward the base — letting strength be folded into the tint with no change to the blend.
     */
    public static int applyStrength(int tint, int mode, float strength) {
        float s = Math.max(0f, Math.min(1f, strength));
        if (s >= 1f) return tint & 0xFFFFFF;
        int id = identity(mode);
        int r = Math.round(((id >> 16) & 0xFF) + (((tint >> 16) & 0xFF) - ((id >> 16) & 0xFF)) * s);
        int g = Math.round(((id >> 8) & 0xFF) + (((tint >> 8) & 0xFF) - ((id >> 8) & 0xFF)) * s);
        int b = Math.round((id & 0xFF) + ((tint & 0xFF) - (id & 0xFF)) * s);
        return (r << 16) | (g << 8) | b;
    }
}
