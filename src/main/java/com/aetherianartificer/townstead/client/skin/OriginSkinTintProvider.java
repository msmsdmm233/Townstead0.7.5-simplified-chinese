package com.aetherianartificer.townstead.client.skin;

import com.aetherianartificer.townstead.client.origin.OriginCatalogClient;
import com.aetherianartificer.townstead.client.origin.OriginClientStore;
import com.aetherianartificer.townstead.origin.GeneCatalogEntry;
import com.aetherianartificer.townstead.origin.OriginCatalogEntry;
import net.conczin.mca.entity.VillagerEntityMCA;

import java.util.OptionalInt;

/**
 * Skin tint from a villager's applied origin: finds the origin's COLOR gene
 * ({@code skin_tone}) and returns its tint colour, which the skin-layer mixin
 * MULTIPLIES over MCA's exact melanin×hemoglobin skin — preserving the full vanilla
 * gradient and shifting it toward the race's palette. A white tint is the identity, so
 * Overworlder (white) runs the very same path and renders pixel-exact vanilla; nothing
 * is special-cased. All inputs are server-synced (origin id, the gene's tint in the
 * catalog), so the colour is identical on every client. Villagers with no applied
 * origin (or no colour gene) keep MCA's native skin.
 */
public final class OriginSkinTintProvider implements SkinTintProvider {

    @Override
    public OptionalInt resolve(VillagerEntityMCA villager) {
        String originId = OriginClientStore.get(villager.getId());
        if (originId.isEmpty()) return OptionalInt.empty();
        OriginCatalogEntry origin = OriginCatalogClient.origin(originId);
        if (origin == null) return OptionalInt.empty();
        for (OriginCatalogEntry.Inherited inherited : origin.inheritedGenes()) {
            GeneCatalogEntry gene = OriginCatalogClient.gene(inherited.geneId());
            if (gene != null && gene.isColor()) {
                int mode = gene.blendMode();
                // Fold strength into the tint (lerp toward the mode's identity); the blend is linear
                // in the tint, so this equals applying the blend at partial strength.
                int eff = SkinBlend.applyStrength(gene.colorFrom(), mode, gene.blendStrength());
                // High byte = blend mode (the skin mixin unpacks it); low 24 = the effective tint RGB.
                return OptionalInt.of((mode << 24) | (eff & 0xFFFFFF));
            }
        }
        return OptionalInt.empty();
    }
}
