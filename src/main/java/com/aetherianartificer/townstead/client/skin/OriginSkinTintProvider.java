package com.aetherianartificer.townstead.client.skin;

import com.aetherianartificer.townstead.client.origin.OriginCatalogClient;
import com.aetherianartificer.townstead.client.origin.OriginClientStore;
import com.aetherianartificer.townstead.origin.GeneCatalogEntry;
import com.aetherianartificer.townstead.origin.OriginCatalogEntry;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Genetics;

import java.util.OptionalInt;

/**
 * Skin tint from a villager's applied origin: finds the origin's COLOR gene
 * ({@code skin_tone}) and lerps its from→to gradient by the villager's melanin
 * roll. All inputs are server-synced (the origin id, the gene's endpoints in the
 * catalog, MCA's melanin), so the colour is identical on every client. Villagers
 * with no applied origin (or no colour gene) keep MCA's native skin.
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
                float t = villager.getGenetics().getGene(Genetics.MELANIN);
                return OptionalInt.of(lerp(gene.colorFrom(), gene.colorTo(), t));
            }
        }
        return OptionalInt.empty();
    }

    /** Linear RGB interpolation, t clamped to [0,1]. */
    private static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }
}
