package com.aetherianartificer.townstead.client.skin;

import net.conczin.mca.entity.VillagerEntityMCA;

import java.util.OptionalInt;

/**
 * Resolves a villager's skin tint as a packed RGB (no alpha), or empty to leave
 * MCA's native skin untouched. Implementations must read only client-visible,
 * server-synced data and be deterministic, so every client computes the same
 * colour for the same villager. Register via {@link SkinTintRegistry}.
 */
public interface SkinTintProvider {

    OptionalInt resolve(VillagerEntityMCA villager);
}
