package com.aetherianartificer.townstead.origin.loot;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Per-origin death loot, keyed by life stage. When a villager or player of an origin dies, the drops for
 * its current stage (or {@link #defaultDrops} when the stage isn't listed) are rolled and spawned IN
 * ADDITION to the entity's normal drops. So an egg can drop a spider egg while an adult drops silk.
 *
 * <p>Loaded from {@code data/<ns>/death_loot/<origin-path>.json}; the file path is the origin id.</p>
 */
public record DeathLootDef(Map<String, List<Drop>> stages, List<Drop> defaultDrops) {

    /** One rolled drop: an {@code item}, a {@code [min,max]} count, and a {@code chance} (0..1) to drop at all. */
    public record Drop(ResourceLocation item, int min, int max, float chance) {}

    /** The drops for a stage id, falling back to {@link #defaultDrops} (possibly empty) when not listed. */
    public List<Drop> dropsForStage(String stageId) {
        if (stageId != null) {
            List<Drop> s = stages.get(stageId);
            if (s != null) return s;
        }
        return defaultDrops;
    }
}
