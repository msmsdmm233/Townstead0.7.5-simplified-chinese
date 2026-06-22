package com.aetherianartificer.townstead.origin.reproduction;

import com.aetherianartificer.townstead.origin.ExpressedGenes;
import com.aetherianartificer.townstead.origin.gene.types.GestationLengthGeneType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-side resolution of the {@code townstead_origins:gestation_length} gene (the mother's pregnancy
 * length multiplier). Defaults to 1.0 (MCA's normal duration). The gestation mixin scales MCA's per-tick
 * {@code babyAge} growth by {@code 1 / multiplier}.
 */
public final class GestationLength {

    private GestationLength() {}

    /** The pregnancy-length multiplier for this mother (> 0; 1.0 = MCA default). */
    public static float forMother(LivingEntity mother) {
        float m = 1f;
        for (GestationLengthGeneType.Instance gene : ExpressedGenes.instancesOf(mother, GestationLengthGeneType.Instance.class)) {
            m = gene.gestationLength();
        }
        return m <= 0f ? 1f : m;
    }
}
