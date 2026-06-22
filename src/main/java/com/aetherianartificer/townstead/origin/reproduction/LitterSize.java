package com.aetherianartificer.townstead.origin.reproduction;

import com.aetherianartificer.townstead.origin.ExpressedGenes;
import com.aetherianartificer.townstead.origin.gene.types.LitterSizeGeneType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-side resolution of the {@code townstead_origins:litter_size} gene (the mother's clutch size).
 * Defaults to 1 (MCA's single child). The litter mixin spawns {@code litterSize - 1} extra children at
 * the pregnancy birth, so an egg-laying species lays a whole clutch at once.
 */
public final class LitterSize {

    private LitterSize() {}

    /** The number of offspring this mother produces per birth (>= 1). */
    public static int forMother(LivingEntity mother) {
        int n = 1;
        for (LitterSizeGeneType.Instance gene : ExpressedGenes.instancesOf(mother, LitterSizeGeneType.Instance.class)) {
            n = Math.max(n, gene.litterSize());
        }
        return Math.max(1, n);
    }
}
