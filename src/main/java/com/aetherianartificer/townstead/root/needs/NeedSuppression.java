package com.aetherianartificer.townstead.root.needs;

import com.aetherianartificer.townstead.root.ExpressedGenes;
import com.aetherianartificer.townstead.root.LifeStage;
import com.aetherianartificer.townstead.root.LifeStageProgression;
import com.aetherianartificer.townstead.root.gene.types.DietGeneType;
import com.aetherianartificer.townstead.root.gene.types.HydrationGeneType;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-side query for needs switched off by a race's diet/hydration genes: a {@code diet:"none"}
 * race does not eat (hunger off), a {@code liquid:"none"} race does not drink (thirst off). Read by
 * the hunger/thirst tickers to skip a need's decay (pinning it full, so the seek behavior never
 * fires). The client mirrors this from the synced gene catalog to hide the need's interact-screen
 * icon (see {@code ClientNeeds}).
 */
public final class NeedSuppression {

    private NeedSuppression() {}

    public static boolean suppressesHunger(LivingEntity entity) {
        if (stageHasNoNeeds(entity)) return true;
        for (DietGeneType.Instance gene : ExpressedGenes.instancesOf(entity, DietGeneType.Instance.class)) {
            if (gene.disablesHunger()) return true;
        }
        return false;
    }

    public static boolean suppressesThirst(LivingEntity entity) {
        if (stageHasNoNeeds(entity)) return true;
        for (HydrationGeneType.Instance gene : ExpressedGenes.instancesOf(entity, HydrationGeneType.Instance.class)) {
            if (gene.disablesThirst()) return true;
        }
        return false;
    }

    /** True when the entity is a villager at a life stage flagged {@code needs:false} (e.g. an egg). */
    private static boolean stageHasNoNeeds(LivingEntity entity) {
        if (!(entity instanceof VillagerEntityMCA villager)) return false;
        LifeStage stage = LifeStageProgression.currentStage(villager);
        return stage != null && !stage.needs();
    }
}
