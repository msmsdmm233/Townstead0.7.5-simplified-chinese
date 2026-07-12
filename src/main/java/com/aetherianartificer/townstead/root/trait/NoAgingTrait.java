package com.aetherianartificer.townstead.root.trait;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Traits;

/**
 * Bridge to MCA's NO_AGING trait (7.6.28+/7.7.18+), the villager editor's age-lock toggle.
 * Townstead's granted-ageless flag ({@code life.ageless()}) stays the runtime freeze
 * authority ({@code LifeStageProgression.isAgeless}); this trait is its player-visible
 * mirror: the agelessness/aging potions write it, and {@code LifeStageTicker} folds
 * editor toggles back into the flag. No-ops on MCA versions without the trait.
 */
public final class NoAgingTrait {

    public static final String ID = "NO_AGING";

    private NoAgingTrait() {}

    /** Whether the villager currently carries the age-lock trait. */
    public static boolean has(VillagerEntityMCA villager) {
        return villager != null && villager.getTraits().hasTrait(ID);
    }

    /** Adds or removes the age-lock trait to mirror {@code locked}. */
    public static void set(VillagerEntityMCA villager, boolean locked) {
        if (villager == null) return;
        Traits.Trait trait = McaTraitResolver.resolve(ID);
        if (trait == null) return;
        if (locked) {
            villager.getTraits().addTrait(trait);
        } else {
            villager.getTraits().removeTrait(trait);
        }
    }
}
