package com.aetherianartificer.townstead.origin.loot;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Server-side registry of {@link DeathLootDef}s keyed by origin id, filled by {@link DeathLootLoader} on
 * data reload and read by {@link DeathLoot} when an entity of that origin dies. Loot is rolled + dropped
 * server-side, so there is no client sync.
 */
public final class DeathLootRegistry {

    private static volatile Map<ResourceLocation, DeathLootDef> byOrigin = Map.of();

    private DeathLootRegistry() {}

    public static void set(Map<ResourceLocation, DeathLootDef> defs) {
        byOrigin = Map.copyOf(defs);
    }

    public static DeathLootDef get(ResourceLocation originId) {
        return originId == null ? null : byOrigin.get(originId);
    }

    public static boolean isEmpty() {
        return byOrigin.isEmpty();
    }
}
