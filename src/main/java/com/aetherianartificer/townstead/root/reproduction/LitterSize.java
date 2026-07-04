package com.aetherianartificer.townstead.root.reproduction;

import com.aetherianartificer.townstead.root.ExpressedGenes;
import com.aetherianartificer.townstead.root.gene.types.LitterSizeGeneType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-side resolution of the {@code townstead_roots:litter_size} gene (the mother's clutch size).
 * Defaults to 1 (MCA's single child). The gene value is the MAXIMUM clutch; the actual count is rolled
 * per birth ({@link #roll}) so a species doesn't lay the same number every time. The litter path then
 * spawns {@code count - 1} extra children beyond MCA's one.
 */
public final class LitterSize {

    private LitterSize() {}

    /** The largest clutch this mother's genes allow (>= 1); the cap that {@link #roll} draws under. */
    public static int max(LivingEntity mother) {
        int n = 1;
        for (LitterSizeGeneType.Instance gene : ExpressedGenes.instancesOf(mother, LitterSizeGeneType.Instance.class)) {
            n = Math.max(n, gene.litterSize());
        }
        return Math.max(1, n);
    }

    /** A randomly-rolled clutch size in {@code [1, max]} for this birth. */
    public static int roll(LivingEntity mother, RandomSource random) {
        int cap = max(mother);
        return cap <= 1 ? 1 : 1 + random.nextInt(cap);
    }
}
