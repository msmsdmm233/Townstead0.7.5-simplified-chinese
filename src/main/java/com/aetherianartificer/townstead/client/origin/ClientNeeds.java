package com.aetherianartificer.townstead.client.origin;

import com.aetherianartificer.townstead.origin.GeneCatalogEntry;

import java.util.Set;

/**
 * Client-side mirror of {@code NeedSuppression}: whether a villager's expressed genes switch off a
 * given need, resolved from the synced per-entity expressed-gene set ({@link OriginClientStore}) and
 * the gene catalog ({@link OriginCatalogClient}). Used by the interact-screen status bar to hide a
 * suppressed need's icon, matching the server which pins that need full.
 */
public final class ClientNeeds {

    private ClientNeeds() {}

    public static boolean suppresses(int entityId, String need) {
        Set<String> geneIds = OriginClientStore.expressedGenes(entityId);
        if (geneIds.isEmpty()) return false;
        for (String geneId : geneIds) {
            GeneCatalogEntry gene = OriginCatalogClient.gene(geneId);
            if (gene != null && gene.suppressesNeed(need)) return true;
        }
        return false;
    }
}
