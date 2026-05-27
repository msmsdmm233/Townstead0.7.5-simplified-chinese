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
}
