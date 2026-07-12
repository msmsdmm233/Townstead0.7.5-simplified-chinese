package com.aetherianartificer.townstead.root.trait;

import net.conczin.mca.entity.ai.Traits;

/**
 * Version-tolerant resolution of MCA-native traits by id. MCA registers traits per
 * version (INFERTILE and NO_AGING are recent); on builds that lack the id,
 * {@code Trait.valueOf} returns the UNKNOWN sentinel (or null on some lines) instead
 * of the requested trait, so the result is only trusted after verifying its id.
 */
public final class McaTraitResolver {

    private McaTraitResolver() {}

    /** The MCA trait registered under {@code id}, or null when this MCA version lacks it. */
    public static Traits.Trait resolve(String id) {
        Traits.Trait trait = Traits.Trait.valueOf(id);
        return trait != null && id.equalsIgnoreCase(trait.id()) ? trait : null;
    }
}
