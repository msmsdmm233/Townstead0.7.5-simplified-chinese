package com.aetherianartificer.townstead.client.origin;

import com.aetherianartificer.townstead.origin.OriginSetC2SPayload;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side current-origin cache, keyed by network entity id (or
 * {@link OriginSetC2SPayload#SELF} for the player's own origin). Fed by
 * {@code OriginSyncS2CPayload}; read live by the picker to highlight the current
 * row and by the skin-tint layer. Also caches each entity's expressed gene ids
 * (fed by {@code ExpressedGenesS2CPayload}) so render layers can paint that
 * individual's genetics. Cleared on logout (see {@code Townstead}).
 */
public final class OriginClientStore {

    private static final Map<Integer, String> BY_ENTITY = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<String>> EXPRESSED = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<String>> TOGGLES = new ConcurrentHashMap<>();

    private OriginClientStore() {}

    public static void set(int entityId, String originId) {
        BY_ENTITY.put(entityId, originId == null ? "" : originId);
    }

    /** Current origin id for the target, or empty string if unknown. */
    public static String get(int entityId) {
        return BY_ENTITY.getOrDefault(entityId, "");
    }

    public static String getSelf() {
        return get(OriginSetC2SPayload.SELF);
    }

    /** Store an entity's expressed gene ids (allele encodings reduced to gene ids). */
    public static void setExpressed(int entityId, List<String> alleleEncodings) {
        Set<String> ids = ConcurrentHashMap.newKeySet();
        for (String encoded : alleleEncodings) {
            if (encoded == null || encoded.isEmpty() || encoded.equals("~")) continue;
            int hash = encoded.indexOf('#');
            ids.add(hash < 0 ? encoded : encoded.substring(0, hash));
        }
        EXPRESSED.put(entityId, ids);
    }

    /** The gene ids the entity expresses, or an empty set if not yet synced. */
    public static Set<String> expressedGenes(int entityId) {
        return EXPRESSED.getOrDefault(entityId, Set.of());
    }

    /** Whether the entity is known to express the given gene id. */
    public static boolean expresses(int entityId, String geneId) {
        return EXPRESSED.getOrDefault(entityId, Set.of()).contains(geneId);
    }

    /** Store the gene ids whose toggle-mode ability is currently ON for the entity. */
    public static void setToggles(int entityId, List<String> geneIds) {
        Set<String> set = ConcurrentHashMap.newKeySet();
        set.addAll(geneIds);
        TOGGLES.put(entityId, set);
    }

    /** Whether the entity's toggle-mode ability gene is currently ON. */
    public static boolean isToggled(int entityId, String geneId) {
        Set<String> set = TOGGLES.get(entityId);
        return set != null && set.contains(geneId);
    }

    /** Drop a single entry; used to evict an editor's throwaway dummy when its screen closes. */
    public static void remove(int entityId) {
        BY_ENTITY.remove(entityId);
        EXPRESSED.remove(entityId);
        TOGGLES.remove(entityId);
    }

    public static void clear() {
        BY_ENTITY.clear();
        EXPRESSED.clear();
        TOGGLES.clear();
    }
}
