package com.aetherianartificer.townstead.client.species;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last skin colour MCA's {@code SkinLayer} rendered per entity, captured by the
 * skin-tint mixin each frame AFTER the Townstead origin tint blends in. This is the
 * exact multiplier the face was drawn with (MCA's melanin x hemoglobin gradient,
 * shifted by the race tint), so skin-tinted attachments (ears, jaws, tails) match
 * the rendered face instead of approximating it from the raw gene tint. An entity
 * whose skin layer never renders stays absent; callers fall back to the resolved
 * tint.
 */
public final class RigSkinColor {

    private static final Map<Integer, Integer> COLORS = new ConcurrentHashMap<>();

    private RigSkinColor() {}

    public static void put(int entityId, int rgb) {
        if (COLORS.size() > 512) COLORS.clear();
        COLORS.put(entityId, rgb & 0xFFFFFF);
    }

    /** The entity's last rendered skin colour (0xRRGGBB), or {@code fallback} when never seen. */
    public static int get(int entityId, int fallback) {
        Integer rgb = COLORS.get(entityId);
        return rgb == null ? fallback : rgb;
    }
}
