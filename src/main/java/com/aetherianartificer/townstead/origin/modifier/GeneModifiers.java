package com.aetherianartificer.townstead.origin.modifier;

import com.aetherianartificer.townstead.origin.ExpressedGenes;
import com.aetherianartificer.townstead.habitus.condition.ConditionContext;
import com.aetherianartificer.townstead.origin.gene.types.ModifierGeneType;
import com.aetherianartificer.townstead.origin.gene.types.ModifierGeneType.Modifier;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Applies an entity's expressed {@code modifier} genes to a server-resolved scalar
 * (healing received, damage dealt, break speed). Each matching gene combines its
 * value with the running result, gated by its optional condition.
 */
public final class GeneModifiers {

    private GeneModifiers() {}

    public static float modify(LivingEntity entity, Modifier kind, float base) {
        if (entity == null || entity.level().isClientSide) return base;
        List<ModifierGeneType.Instance> genes =
                ExpressedGenes.instancesOf(entity, ModifierGeneType.Instance.class);
        if (genes.isEmpty()) return base;
        float result = base;
        ConditionContext ctx = null;
        for (ModifierGeneType.Instance gene : genes) {
            if (gene.modifier() != kind) continue;
            if (gene.condition() != null) {
                if (ctx == null) ctx = new ConditionContext(entity);
                if (!gene.condition().test(ctx)) continue;
            }
            result = gene.applyTo(result);
        }
        return result;
    }
}
