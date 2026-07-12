package com.aetherianartificer.townstead.client.species;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last hair colour MCA's {@code HairLayer} rendered per entity, captured by
 * the hair mixin each frame (post senior-greying). Hair colour is MCA villager
 * data, not a gene, and the layer is the one place both versions resolve it —
 * so consumers (hair-tinted attachments) read this store instead of re-deriving
 * it. An entity whose rig hides hair never renders the layer and stays absent;
 * callers fall back to their flat tint.
 */
public final class RigHairColor {

    private static final Map<Integer, Integer> COLORS = new ConcurrentHashMap<>();

    private RigHairColor() {}

    public static void put(int entityId, int rgb) {
        if (COLORS.size() > 512) COLORS.clear();
        COLORS.put(entityId, rgb & 0xFFFFFF);
    }

    /** The entity's last rendered hair colour (0xRRGGBB), or {@code fallback} when never seen. */
    public static int get(int entityId, int fallback) {
        Integer rgb = COLORS.get(entityId);
        return rgb == null ? fallback : rgb;
    }
}
